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
)

type Msg struct {
	Iv      string `json:"iv"`
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

// utility functions
func updateCounter(id string, s *mgo.Session) int {
	var counter Counter
	col := s.DB("muteswan").C("counters")
	change := mgo.Change{Update: bson.M{"$inc": bson.M{"n": 1}}, New: true, Upsert: true}
	err := col.Find(bson.M{"_id": "C" + id}).Modify(change, &counter)
	if err != nil {
		panic("Failed to update counter")
	}
	fmt.Println(counter.N)
	return counter.N
}

func getCounter(id string, s *mgo.Session) int {
	var counter Counter
	col := s.DB("muteswan").C("counters")
	err := col.Find(bson.M{"_id": "C" + id}).One(&counter)
	if err != nil {
		return(0)
	}
	fmt.Println(counter.N)
	return counter.N
}

func PostMsg(c *goweb.Context, s *mgo.Session) {
	var m Msg
	var mw MsgWrap

	col := s.DB("muteswan").C("C" + c.PathParams["hash"])
	body, _ := ioutil.ReadAll(c.Request.Body)
	json.Unmarshal(body, &m)
	mw.Content = m
	mw.Id = updateCounter(c.PathParams["hash"], s)
	mw.Timestamp = time.Now().Format(time.RFC1123)
	mw.Timestamp = strings.Replace(mw.Timestamp,"UTC","GMT",-1)
	col.Insert(mw)
}

func GetMsg(c *goweb.Context, s *mgo.Session) {

	if strings.Contains(c.PathParams["id"], "-") {

		ids := strings.Split(c.PathParams["id"], "-")
		top, _ := strconv.Atoi(ids[0])
		bottom, _ := strconv.Atoi(ids[1])
		if bottom > top {
			fmt.Fprintf(c.ResponseWriter, "wtf %d to %d make no sense", top, bottom)
			return
		}

		var msgs []MsgWrap
		col := s.DB("muteswan").C("C" + c.PathParams["hash"])
		//msgQuery := col.Find(bson.M{"_id": bson.M{"$lte": top, "$gte": bottom}})
		msgQuery := col.Find(bson.M{"_id": bson.M{"$gte": bottom, "$lte": top}}).Sort("-_id")
		msgQuery.All(&msgs)

		msgBytes, _ := json.Marshal(msgs)
		buf := bytes.NewBuffer(msgBytes)
		fmt.Fprintf(c.ResponseWriter, "%s", buf.String())

	} else {
		var mw MsgWrap
		col := s.DB("muteswan").C("C" + c.PathParams["hash"])
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

func GetLastMsg(c *goweb.Context, s *mgo.Session) {

	mostRecent := getCounter(c.PathParams["hash"], s)

	fmt.Fprintf(c.ResponseWriter, `{"lastMessage":"%d"}`, mostRecent)
}

func main() {

	fmt.Printf("Max procs: %d\n", runtime.GOMAXPROCS(8))
	fmt.Printf("Max procs: %d\n", runtime.GOMAXPROCS(8))

	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	session.SetMode(mgo.Monotonic, true)

	goweb.MapFunc("/{hash}/{id}", func(c *goweb.Context) {
		//fmt.Printf("GET %s\n", c.Request.URL.String())
		s := session.Copy()
		GetMsg(c, s)
		s.Close()
	})

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		//fmt.Printf("GET %s\n", c.Request.URL.String())
		s := session.Copy()
		GetLastMsg(c, s)
		s.Close()
	}, goweb.GetMethod)

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		//fmt.Printf("POST %s\n", c.Request.URL.String())
		s := session.Copy()
		PostMsg(c, s)
		s.Close()
	}, goweb.PostMethod)

	goweb.ListenAndServe(":8081")

}
