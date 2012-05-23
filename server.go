package main

import (
	"bufio"
	"bytes"
	"code.google.com/p/goweb/goweb"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"launchpad.net/mgo"
	"launchpad.net/mgo/bson"
	"os"
	"strconv"
	"strings"
	"time"
)


type Msg struct {
	Iv      string `json:"iv"`
	Message string `json:"message"`
}

type MsgWrap struct {
	Content   Msg    `json:"content"`
	Timestamp string `json:"timestamp"`
	Id        int `bson:"_id"`
}

type Counter struct {
	Id string `bson:"_id"`
	N int
}

// utility functions
func ReadFileContents(file *os.File) string {
	reader := bufio.NewReader(file)
	rawBytes, _ := ioutil.ReadAll(reader)
	buffer := bytes.NewBuffer(rawBytes)
	return (buffer.String())
}

func updateCounter(id string, s *mgo.Session) int {
	var counter Counter
	col := s.DB("muteswan").C("counters")
	change := mgo.Change{Update: bson.M{"$inc": bson.M{"n": 1}}, New: true, Upsert: true}
	err := col.Find(bson.M{"_id": id}).Modify(change, &counter)
	if err != nil {
		panic("Failed to update counter")
	}
	fmt.Println(counter.N)
	return counter.N
}

func GetLatestMsgFile(hash string) int {
	path := fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s", hash)
	dir, _ := os.Open(path)
	files, _ := dir.Readdirnames(10000)
	var mostRecent int
	for i := range files {
		id, _ := strconv.Atoi(files[i])
		if id > mostRecent {
			mostRecent = id
		}

	}
	return (mostRecent)
}

func GetLatestMsg(hash string, s *mgo.Session) int {
	//c := s.DB("muteswan").C(hash)
	return 0
}


func PostMsgFile(c *goweb.Context, s *mgo.Session) {
	mostRecent := GetLatestMsg(c.PathParams["hash"],s)
	msgId := mostRecent + 1
	filepath := fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s/%d", c.PathParams["hash"], msgId)
	file, _ := os.Create(filepath)
	wr := bufio.NewWriter(file)
	io.Copy(wr, c.Request.Body)
	wr.Flush()
	//fmt.Fprintf(c.ResponseWriter, "Wrote %d bytes.", written)
}

func PostMsg(c *goweb.Context, s *mgo.Session) {
	var m Msg
	var mw MsgWrap

	col := s.DB("muteswan").C(c.PathParams["hash"])
	body,_ := ioutil.ReadAll(c.Request.Body)
	json.Unmarshal(body, &m)
	mw.Content = m
	mw.Id = updateCounter(c.PathParams["hash"],s)
	mw.Timestamp = time.Now().Format(time.RFC1123)
	col.Insert(mw)
}


func GetMsgFile(c *goweb.Context, s *mgo.Session) {

	if strings.Contains(c.PathParams["id"], "-") {

		ids := strings.Split(c.PathParams["id"], "-")
		top, _ := strconv.Atoi(ids[0])
		bottom, _ := strconv.Atoi(ids[1])
		if bottom > top {
			fmt.Fprintf(c.ResponseWriter, "wtf %d to %d make no sense", top, bottom)
			return
		}

		var msgs []MsgWrap
		for i := top; i >= bottom; i-- {
			var m Msg
			var mw MsgWrap
			//var m interface{}
			path := fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s/%d", c.PathParams["hash"], i)
			file, err := os.Open(path)
			if err != nil {
				fmt.Fprintf(c.ResponseWriter, "wtf not found %d", i)
				return
			}
			jsonString := ReadFileContents(file)
			bytes := []byte(jsonString)
			err = json.Unmarshal(bytes, &m)
			mw.Content = m

			stat, _ := file.Stat()
			mw.Timestamp = stat.ModTime().Format(time.RFC1123)
			msgs = append(msgs, mw)
		}

		msgBytes, _ := json.Marshal(msgs)
		buf := bytes.NewBuffer(msgBytes)
		fmt.Fprintf(c.ResponseWriter, "%s", buf.String())
		//c.RespondWithdata(msgs)

	} else {
		//fmt.Fprintf(c.ResponseWriter, "PathParams without dash %s\n", c.PathParams)
		var (
			file *os.File
			path string
		)
		path = fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s/%s", c.PathParams["hash"], c.PathParams["id"])
		file, _ = os.Open(path)
		stat, _ := file.Stat()

		jsonString := ReadFileContents(file)

		c.ResponseWriter.Header().Set("Last-Modified", stat.ModTime().String())

		// uh..fix this to be less stupid
		fmt.Fprintf(c.ResponseWriter, "%s", jsonString)
		//c.RespondWithdata(jsonString)
	}

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
                for i := top; i >= bottom; i-- {
                        var m Msg
                        var mw MsgWrap
                        //var m interface{}
                        path := fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s/%d",
c.PathParams["hash"], i)
                        file, err := os.Open(path)
                        if err != nil {
                                fmt.Fprintf(c.ResponseWriter, "wtf not found %d", i)
                                return
                        }
                        jsonString := ReadFileContents(file)
                        bytes := []byte(jsonString)
                        err = json.Unmarshal(bytes, &m)
                        mw.Content = m

                        stat, _ := file.Stat()
                        mw.Timestamp = stat.ModTime().Format(time.RFC1123)
                        msgs = append(msgs, mw)
                }

                msgBytes, _ := json.Marshal(msgs)
                buf := bytes.NewBuffer(msgBytes)
                fmt.Fprintf(c.ResponseWriter, "%s", buf.String())
                //c.RespondWithdata(msgs)

        } else {
                //fmt.Fprintf(c.ResponseWriter, "PathParams without dash %s\n", c.PathParams)
                var (
                        file *os.File
                        path string
                )
                path = fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s/%s", c.PathParams["hash"], c.PathParams["id"])
                file, _ = os.Open(path)
                stat, _ := file.Stat()

                jsonString := ReadFileContents(file)

                c.ResponseWriter.Header().Set("Last-Modified", stat.ModTime().String())

                // uh..fix this to be less stupid
                fmt.Fprintf(c.ResponseWriter, "%s", jsonString)
                //c.RespondWithdata(jsonString)
        }

}


func GetLastMsg(c *goweb.Context, s *mgo.Session) {

	mostRecent := GetLatestMsg(c.PathParams["hash"],s)

	// use interface and unmarshal?
	fmt.Fprintf(c.ResponseWriter, `{"lastMessage":"%d"}`, mostRecent)
}

func main() {

	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()


	session.SetMode(mgo.Monotonic, true)

	goweb.MapFunc("/{hash}/{id}", func(c *goweb.Context) {
		fmt.Printf("URL: %s\n", c.Request.URL.String())
		GetMsg(c,session)
	})

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		fmt.Printf("URL: %s\n", c.Request.URL.String())
		GetLastMsg(c,session)
	}, goweb.GetMethod)
	//goweb.MapFunc("/{hash}", getLastMsg)

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		fmt.Printf("URL: %s\n", c.Request.URL.String())
		PostMsg(c,session)
	}, goweb.PostMethod)

	goweb.ListenAndServe(":8081")

}
