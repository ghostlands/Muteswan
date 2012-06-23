package main

import (
	"bytes"
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

func GetMsg(c *goweb.Context, d *mgo.Database) {

	if strings.Contains(c.PathParams["id"], "-") {

		ids := strings.Split(c.PathParams["id"], "-")
		top, _ := strconv.Atoi(ids[0])
		bottom, _ := strconv.Atoi(ids[1])
		if bottom > top {
			fmt.Fprintf(c.ResponseWriter, "wtf %d to %d make no sense", top, bottom)
			return
		}

		var msgs []MsgWrap
		col := d.C("C" + c.PathParams["hash"])
		//msgQuery := col.Find(bson.M{"_id": bson.M{"$lte": top, "$gte": bottom}})
		msgQuery := col.Find(bson.M{"_id": bson.M{"$gte": bottom, "$lte": top}}).Sort("-_id")
		msgQuery.All(&msgs)

		msgBytes, _ := json.Marshal(msgs)
		buf := bytes.NewBuffer(msgBytes)
		fmt.Fprintf(c.ResponseWriter, "%s", buf.String())

	} else {
		var mw MsgWrap
		col := d.C("C" + c.PathParams["hash"])
		id, _ := strconv.Atoi(c.PathParams["id"])
		err := col.Find(bson.M{"_id": id}).One(&mw)
		if err != nil {
			fmt.Printf("Error fetching %s: %s", c.PathParams["hash"], err)
		}

		c.ResponseWriter.Header().Set("Last-Modified", mw.Timestamp)
		msgBytes, _ := json.Marshal(mw.Content)
		buf := bytes.NewBuffer(msgBytes)
		fmt.Fprintf(c.ResponseWriter, "%s", buf.String())
	}

}

func GetLastMsg(c *goweb.Context, d *mgo.Database) {

	mostRecent := getCounter(c.PathParams["hash"], d)

	lastMsg := &LastMessage{LastMessage : mostRecent}


	b,_ := json.Marshal(lastMsg)
	buf := bytes.NewBuffer(b)
	fmt.Fprintf(c.ResponseWriter,"%s",buf.String())
	//fmt.Fprintf(c.ResponseWriter, `{"lastMessage":"%d"}`, mostRecent)
}

func dropPrivs() {
	uid := 1001

	//fmt.Printf("Current uid: %d\n", syscall.Getuid())
	syscall.Setuid(uid)
	//fmt.Printf("New uid: %d\n", syscall.Getuid())

}

func main() {

	fmt.Printf("Max procs: %d\n", runtime.GOMAXPROCS(8))
	fmt.Printf("Max procs: %d\n", runtime.GOMAXPROCS(8))

	var port int
	var ip string
	var db string
	flag.IntVar(&port, "port", 80, "Port to bind on.")
	flag.StringVar(&ip, "ip", "127.0.0.1", "IP to bind to")
	flag.StringVar(&db, "db", "muteswan", "MongoDB database to use")
	flag.Parse()

	fmt.Printf("Port is %d\n", port)
	fmt.Printf("IP is %s\n", ip)
	fmt.Printf("DB is %s\n", db)


	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	session.SetMode(mgo.Monotonic, true)

	goweb.MapFunc("/{hash}/{id}", func(c *goweb.Context) {
		dropPrivs()
		fmt.Printf("GET %s\n", c.Request.URL.String())
		s := session.Copy()
		GetMsg(c, s.DB(db))
		s.Close()
	})

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		dropPrivs()
		fmt.Printf("GET %s\n", c.Request.URL.String())
		s := session.Copy()
		GetLastMsg(c, s.DB(db))
		s.Close()
	}, goweb.GetMethod)

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		dropPrivs()
		fmt.Printf("POST %s\n", c.Request.URL.String())
		s := session.Copy()
		PostMsg(c, s.DB(db))
		s.Close()
	}, goweb.PostMethod)

	goweb.ListenAndServe(fmt.Sprintf("%s:%d",ip,port))

}
