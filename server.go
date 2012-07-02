package main

import (
	"code.google.com/p/goweb/goweb"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"launchpad.net/mgo"
	"launchpad.net/mgo/bson"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"syscall"
	"time"
)

// types
type Msg struct {
	Iv      string `json:"iv,omitempty"`
	Message string `json:"message"`
}

type MsgWrap struct {
	Content   Msg       `json:"content"`
	Timestamp string    `json:"timestamp"`
	Time      time.Time `json:"-"`
	Id        int       `bson:"_id"`
}

type Counter struct {
	Id string `bson:"_id"`
	N  int
}

type LastMessage struct {
	LastMessage int `json:"lastMessage"`
}

// utility functions
func updateCounter(id string, d *mgo.Database) int {
	var counter Counter
	col := d.C("counters")
	change := mgo.Change{Update: bson.M{"$inc": bson.M{"n": 1}}, New: true, Upsert: true}
	err := col.Find(bson.M{"_id": "C" + id}).Modify(change, &counter)
	if err != nil {
		panic("Failed to update counter")
	}
	fmt.Println(counter.N)
	return counter.N
}

func getCounter(id string, d *mgo.Database) int {
	var counter Counter
	col := d.C("counters")
	err := col.Find(bson.M{"_id": "C" + id}).One(&counter)
	if err != nil {
		return (0)
	}
	fmt.Println(counter.N)
	return counter.N
}

func dropPrivs(uid int) {
	if syscall.Getuid() == 0 {
		fmt.Printf("Dropping privileges.")
		syscall.Setuid(uid)
	}
}

func validateHash(hash string) {
	match, _ := regexp.MatchString("^\\w{40}$", hash)
	if !match {
		panic("Invalid hash circle key.")
	}
}

func catchPanic(c *goweb.Context, s *mgo.Session) {

	if r := recover(); r != nil {
		fmt.Printf("%s\n", r)
		s.Close()
		goweb.AddFormatter(&goweb.JsonFormatter{})
		c.RespondWithError(400)
		recover()
	}
	s.Close()
}

// handler functions
func PostMsg(c *goweb.Context, d *mgo.Database) {
	var m Msg
	var mw MsgWrap

	col := d.C("C" + c.PathParams["hash"])
	body, _ := ioutil.ReadAll(c.Request.Body)
	json.Unmarshal(body, &m)
	mw.Content = m
	mw.Id = updateCounter(c.PathParams["hash"], d)
	mw.Time = time.Now()
	mw.Timestamp = mw.Time.Format(time.RFC1123)
	mw.Timestamp = strings.Replace(mw.Timestamp, "UTC", "GMT", -1)
	col.Insert(mw)
}

func GetMsgRange(c *goweb.Context, d *mgo.Database) {
	ids := strings.Split(c.PathParams["id"], "-")
	top, _ := strconv.Atoi(ids[0])
	bottom, _ := strconv.Atoi(ids[1])

	if bottom > top {
		goweb.AddFormatter(&goweb.JsonFormatter{})
		c.RespondWithStatus(403)
		return
	}

	delta := top - bottom
	if delta > 30 {
		goweb.AddFormatter(&goweb.JsonFormatter{})
		c.RespondWithStatus(403)
		return
	}

	var msgs []MsgWrap
	col := d.C("C" + c.PathParams["hash"])
	msgQuery := col.Find(bson.M{"_id": bson.M{"$gte": bottom, "$lte": top}}).Sort("-_id")
	msgQuery.All(&msgs)

	msgBytes, _ := json.Marshal(msgs)
	c.ResponseWriter.Header().Set("Content-Type", "application/json")
	c.ResponseWriter.Write(msgBytes)
}

func GetOneMsg(c *goweb.Context, d *mgo.Database) {
	var mw MsgWrap
	col := d.C("C" + c.PathParams["hash"])
	id, _ := strconv.Atoi(c.PathParams["id"])
	err := col.Find(bson.M{"_id": id}).One(&mw)
	if err != nil {
		fmt.Printf("Error fetching %s: %s", c.PathParams["hash"], err)
		goweb.AddFormatter(&goweb.JsonFormatter{})
		c.RespondWithNotFound()
		return
	}

	c.ResponseWriter.Header().Set("Last-Modified", mw.Timestamp)
	msgBytes, _ := json.Marshal(mw.Content)
	c.ResponseWriter.Header().Set("Content-Type", "application/json")
	c.ResponseWriter.Write(msgBytes)
}

func GetMsg(c *goweb.Context, d *mgo.Database) {

	if strings.Contains(c.PathParams["id"], "-") {
		GetMsgRange(c, d)
	} else {
		GetOneMsg(c, d)
	}

}

func GetLastMsg(c *goweb.Context, d *mgo.Database) {

	mostRecent := getCounter(c.PathParams["hash"], d)

	lastMsg := &LastMessage{LastMessage: mostRecent}

	b, _ := json.Marshal(lastMsg)
	c.ResponseWriter.Header().Set("Content-Type", "application/json")
	c.ResponseWriter.Write(b)
}

// expire goroutine
func ExpireMessageLoop(d *mgo.Database) {
	for true {
		fmt.Printf("Searching for expired messages...\n")
		collections, _ := d.CollectionNames()
		for _, collection := range collections {
			if !strings.HasPrefix(collection, "C") {
				continue
			}

			var msgs []MsgWrap
			fmt.Printf("Expiring %s\n", collection)
			col := d.C(collection)

			// 3  months old and they are gone
			delBoundary := time.Now().Add(-time.Hour * 2160)
			msgQuery := col.Find(bson.M{"time": bson.M{"$lte": delBoundary}})
			msgQuery.All(&msgs)

			for _, msgW := range msgs {
				fmt.Printf("Deleted id %d timestamp %s\n", msgW.Id, msgW.Timestamp)
				err := col.Remove(bson.M{"_id": msgW.Id})
				if err != nil {
					fmt.Printf("Error expiring %d in %s.", msgW.Id, collection)
					continue
				}
			}

		}
		time.Sleep(time.Hour)
	}
}

func main() {

	runtime.GOMAXPROCS(runtime.NumCPU())

	var (
		port int
		ip   string
		db   string
		uid  int
	)
	flag.IntVar(&port, "port", 80, "Port to bind on.")
	flag.StringVar(&ip, "ip", "127.0.0.1", "IP to bind to")
	flag.StringVar(&db, "db", "muteswan", "MongoDB database to use")
	flag.IntVar(&uid, "uid", 1001, "User to drop privileges")
	flag.Parse()

	fmt.Printf("Port is %d\n", port)
	fmt.Printf("IP is %s\n", ip)
	fmt.Printf("DB is %s\n", db)
	fmt.Printf("UID is %d\n", uid)

	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	session.SetMode(mgo.Monotonic, true)

	go ExpireMessageLoop(session.DB(db))

	// fetch a message, or a message range
	// e.g. GET /1e7db1c00e4036d9ea426e5875c355184578ab2d/200
	// or   GET /1e7db1c00e4036d9ea426e5875c355184578ab2d/200-190
	goweb.MapFunc("/{hash}/{id}", func(c *goweb.Context) {
		s := session.Copy()
		defer catchPanic(c, s)
		dropPrivs(uid)

		validateHash(c.PathParams["hash"])
		fmt.Printf("GET %s\n", c.Request.URL.String())
		GetMsg(c, s.DB(db))
	})

	// Get the last message in the circle
	// e.g. GET /1e7db1c00e4036d9ea426e5875c355184578ab2d
	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		s := session.Copy()
		defer catchPanic(c, s)
		dropPrivs(uid)

		validateHash(c.PathParams["hash"])
		fmt.Printf("GET %s\n", c.Request.URL.String())
		GetLastMsg(c, s.DB(db))
	}, goweb.GetMethod)

	// POST a new message to a circle
	// e.g. POST /1e7db1c00e4036d9ea426e5875c355184578ab2d
	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		s := session.Copy()
		defer catchPanic(c, s)
		dropPrivs(uid)

		validateHash(c.PathParams["hash"])
		fmt.Printf("POST %s\n", c.Request.URL.String())
		PostMsg(c, s.DB(db))
	}, goweb.PostMethod)

	goweb.ListenAndServe(fmt.Sprintf("%s:%d", ip, port))

}
