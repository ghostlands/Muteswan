package main

import (
	"code.google.com/p/goweb/goweb"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"launchpad.net/mgo"
	"launchpad.net/mgo/bson"
	"strconv"
	"strings"
	"time"
	"runtime"
	"syscall"
	"flag"
	"regexp"
)

type Msg struct {
	Iv      string `json:"iv,omitempty"`
	Message string `json:"message"`
}

type MsgWrap struct {
	Content   Msg    `json:"content"`
	Timestamp string `json:"timestamp"`
	Id        int    `bson:"_id"`
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
		return(0)
	}
	fmt.Println(counter.N)
	return counter.N
}

func PostMsg(c *goweb.Context, d *mgo.Database) {
	var m Msg
	var mw MsgWrap

	col := d.C("C" + c.PathParams["hash"])
	body, _ := ioutil.ReadAll(c.Request.Body)
	json.Unmarshal(body, &m)
	mw.Content = m
	mw.Id = updateCounter(c.PathParams["hash"], d)
	mw.Timestamp = time.Now().Format(time.RFC1123)
	mw.Timestamp = strings.Replace(mw.Timestamp,"UTC","GMT",-1)
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
		c.ResponseWriter.Write(msgBytes)
		//buf := bytes.NewBuffer(msgBytes)
		//fmt.Fprintf(c.ResponseWriter, "%s", buf.String())
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
		c.ResponseWriter.Write(msgBytes)
		//buf := bytes.NewBuffer(msgBytes)
		//fmt.Fprintf(c.ResponseWriter, "%s", buf.String())
}


func GetMsg(c *goweb.Context, d *mgo.Database) {



	if strings.Contains(c.PathParams["id"], "-") {
		GetMsgRange(c,d)
	} else {
		GetOneMsg(c,d)
	}

}

func GetLastMsg(c *goweb.Context, d *mgo.Database) {

	mostRecent := getCounter(c.PathParams["hash"], d)

	lastMsg := &LastMessage{LastMessage : mostRecent}


	b,_ := json.Marshal(lastMsg)
	c.ResponseWriter.Write(b)
	//fmt.Fprintf(c.ResponseWriter,"%s",bytes.NewBuffer(b).String())
}

func dropPrivs(uid int) {
	if syscall.Getuid() == 0 {
		fmt.Printf("Dropping privileges.")
		syscall.Setuid(uid)
	}
}

func validateHash(hash string) {
	match,_ := regexp.MatchString("^\\w{40}$",hash)
	if !match {
		panic("Invalid hash circle key.")
	}
}

func catchPanic(c *goweb.Context) {

	if r := recover(); r != nil {
		fmt.Printf("%s\n",r)
		goweb.AddFormatter(&goweb.JsonFormatter{})
		c.RespondWithError(400)
		recover()
        }
}

func main() {

	fmt.Printf("Max procs: %d\n", runtime.GOMAXPROCS(8))
	fmt.Printf("Max procs: %d\n", runtime.GOMAXPROCS(8))


	var (
	   port int
	   ip string
	   db string
	   uid int
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

	goweb.MapFunc("/{hash}/{id}", func(c *goweb.Context) {
		defer catchPanic(c)
		dropPrivs(uid)

		validateHash(c.PathParams["hash"])
		fmt.Printf("GET %s\n", c.Request.URL.String())
		s := session.Copy()
		GetMsg(c, s.DB(db))
		//fmt.Printf("uid: %d\n",syscall.Getuid())
		s.Close()
	})

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		defer catchPanic(c)
		dropPrivs(uid)

		validateHash(c.PathParams["hash"])
		fmt.Printf("GET %s\n", c.Request.URL.String())
		s := session.Copy()
		GetLastMsg(c, s.DB(db))
		//fmt.Printf("uid: %d\n",syscall.Getuid())
		s.Close()
	}, goweb.GetMethod)

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		defer catchPanic(c)
		dropPrivs(uid)

		validateHash(c.PathParams["hash"])
		fmt.Printf("POST %s\n", c.Request.URL.String())
		s := session.Copy()
		PostMsg(c, s.DB(db))
		//fmt.Printf("uid: %d\n",syscall.Getuid())
		s.Close()
	}, goweb.PostMethod)

	goweb.ListenAndServe(fmt.Sprintf("%s:%d",ip,port))

}
