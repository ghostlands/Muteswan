package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"labix.org/v2/mgo"
	"labix.org/v2/mgo/bson"
	"os"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"time"
	"net/http"
)

// types
type Msg struct {
	Iv      string `json:"iv,omitempty"`
	Message string `json:"message"`
}

type ServerInfo struct {
	Name string
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

type MtsnStore interface {
	GetMsg(id int) (MsgWrap, error)
	GetMsgs(top int, bottom int) ([]MsgWrap, error)
	GetLastMsg() (LastMessage, error)
	PostMsg(msgw MsgWrap) error
}

type MongoStore struct {
	Circle string
	Db     *mgo.Database
}

type FileStore struct {
	Circle  string
	Datadir string
	Lock chan int
}

// mongodb implementation of MtsnStore
func (ms *MongoStore) getCounter() int {
	var counter Counter
	col := ms.Db.C("counters")
	err := col.Find(bson.M{"_id": "C" + ms.Circle}).One(&counter)
	if err != nil {
		return (0)
	}
	return counter.N
}

func (ms *MongoStore) updateCounter() int {
	var counter Counter
	col := ms.Db.C("counters")
	change := mgo.Change{Update: bson.M{"$inc": bson.M{"n": 1}}, Remove: false, ReturnNew: true, Upsert: true}
	_, err := col.Find(bson.M{"_id": "C" + ms.Circle}).Apply(change, &counter)
	if err != nil {
		panic("Failed to update counter")
	}
	return counter.N
}

func (ms *MongoStore) GetMsg(id int) (MsgWrap, error) {
	var mw MsgWrap
	col := ms.Db.C("C" + ms.Circle)

	fmt.Printf("Finding in %d\n",id)
	err := col.Find(bson.M{"_id": id}).One(&mw)
	if err != nil {
		fmt.Printf("Error fetching %s: %s", id, err)
		return mw, err
	}

	return mw, nil
}

func (ms *MongoStore) GetMsgs(top int, bottom int) ([]MsgWrap, error) {
	var msgs []MsgWrap

	if bottom > top {
		return msgs, nil
	}

	col := ms.Db.C("C" + ms.Circle)
	msgQuery := col.Find(bson.M{"_id": bson.M{"$gte": bottom, "$lte": top}}).Sort("-_id")
	err := msgQuery.All(&msgs)

	if err != nil {
		return msgs, err
	}

	return msgs, nil
}

func (ms *MongoStore) GetLastMsg() (LastMessage, error) {
	var lastMessage LastMessage
	mostRecent := ms.getCounter()
	lastMessage = LastMessage{LastMessage: mostRecent}
	return lastMessage, nil
}

func (ms *MongoStore) PostMsg(msgw MsgWrap) error {

	msgw.Id = ms.updateCounter()
	msgw.Time = time.Now()
	fmt.Printf("new Id %d\n", msgw.Id)
	msgw.Timestamp = msgw.Time.UTC().Format(time.RFC1123)
	msgw.Timestamp = strings.Replace(msgw.Timestamp, "UTC", "GMT", -1)

	col := ms.Db.C("C" + ms.Circle)
	err := col.Insert(msgw)

	if err != nil {
		fmt.Printf("Error inserting message %s\n", err)
		return err
	}
	fmt.Printf("Posted %s\n", msgw.Content.Message)

	return nil

}

// Filestore implementation of MtsnStore
func ReadFileContents(file *os.File) string {
	reader := bufio.NewReader(file)
	rawBytes, _ := ioutil.ReadAll(reader)
	buffer := bytes.NewBuffer(rawBytes)
	return (buffer.String())
}

func (ms *FileStore) GetMsg(id int) (MsgWrap, error) {

	var (
		file *os.File
		path string
		mw   MsgWrap
	)
	path = fmt.Sprintf("%s%s%s%s%d", ms.Datadir, string(os.PathSeparator), ms.Circle, os.PathSeparator, id)
	file, _ = os.Open(path)
	stat, _ := file.Stat()

	jsonBytes := []byte(ReadFileContents(file))
	json.Unmarshal(jsonBytes, &mw.Content)
	mw.Time = stat.ModTime()
	mw.Id = id
	mw.Timestamp = mw.Time.UTC().Format(time.RFC1123)
	mw.Timestamp = strings.Replace(mw.Timestamp, "UTC", "GMT", -1)

	return mw, nil

}

func (ms *FileStore) GetMsgs(top int, bottom int) ([]MsgWrap, error) {
	var msgs []MsgWrap

	if bottom > top {
		return msgs, nil
	}

	for i := top; i >= bottom; i-- {
		var m Msg
		var mw MsgWrap
		path := fmt.Sprintf("%s/%s/%d", ms.Datadir, ms.Circle, i)
		file, err := os.Open(path)
		if err != nil {
			return msgs, nil
		}

		jsonString := ReadFileContents(file)
		bytes := []byte(jsonString)
		err = json.Unmarshal(bytes, &m)
		mw.Content = m

		stat, _ := file.Stat()
		mw.Time = stat.ModTime()
		mw.Id = i
		mw.Timestamp = mw.Time.UTC().Format(time.RFC1123)
		mw.Timestamp = strings.Replace(mw.Timestamp, "UTC", "GMT", -1)

		msgs = append(msgs, mw)
	}

	return msgs, nil
}

func (ms *FileStore) GetLastMsg() (LastMessage, error) {

	path := fmt.Sprintf("%s/%s", ms.Datadir, ms.Circle)
	dir, err := os.Open(path)

	if err != nil {
		lastMsg := LastMessage{LastMessage: 0}
		return lastMsg, nil
	}

	files, _ := dir.Readdirnames(10000)
	var mostRecent int
	for i := range files {
		id, _ := strconv.Atoi(files[i])
		if id > mostRecent {
			mostRecent = id
		}

	}

	lastMsg := LastMessage{LastMessage: mostRecent}
	return lastMsg, nil
}

func (ms *FileStore) updateCounter() (int) {
	lastMsg,_ := ms.GetLastMsg()
	return lastMsg.LastMessage + 1
}


func (ms *FileStore) PostMsg(msgw MsgWrap) error {

	circledir := fmt.Sprintf("%s%s%s", ms.Datadir, string(os.PathSeparator), ms.Circle)
	fmt.Printf("Using dir: %s\n",circledir)

	_,err := os.Stat(circledir)
	if err != nil {
		fmt.Printf("Error stating dr: %s\n",err)
		err = os.Mkdir(circledir,0700)
		if err != nil {
			return err
		}
	}

	fmt.Printf("Acquiring Lock...\n")
	lock := <- ms.Lock
	fmt.Printf("Got lock %d\n", lock)
	if lock == 0 {
		msgId := ms.updateCounter();

		filepath := fmt.Sprintf("%s/%s/%d", ms.Datadir, ms.Circle, msgId)
		file, err := os.Create(filepath)
		if err != nil {
			go Unlock(ms.Lock)
			fmt.Printf("Failed to write to %s\n", filepath)
			return err
		}
		msgBytes, _ := json.Marshal(msgw.Content)
		wr := bufio.NewWriter(file)
		wr.Write(msgBytes)
		wr.Flush()
		file.Close()
		fmt.Printf("Releasing lock\n")
		go Unlock(ms.Lock)
	}


	return nil
}

// misc functions
/*
func dropPrivs(uid int) {
	if syscall.Getuid() == 0 {
		fmt.Printf("Dropping privileges.")
		syscall.Setuid(uid)
	}
}
*/

func validateHash(hash string) {
	matchSha1, _ := regexp.MatchString("^\\w{40}$", hash)
	matchSha256, _ := regexp.MatchString("^\\w{64}$", hash)
	if !matchSha1 || !matchSha256 {
		panic("Invalid hash circle key.")
	}
}

func makeStore( dbtype, db, hash string, s *mgo.Session, lock chan int) MtsnStore {


	if dbtype == "mongo" {
		return &MongoStore{Circle: hash, Db: s.DB(db)}
	}

	if dbtype == "file" {
		return &FileStore{Circle: hash, Datadir: db, Lock: lock}
	}

	return nil

}

// handler functions
////////////////////
func PostMsg(w http.ResponseWriter, r *http.Request, store MtsnStore) {
	var m Msg
	var mw MsgWrap

	body, err := ioutil.ReadAll(r.Body)
	if err != nil || len(body) == 0 {
		http.Error(w,"Empty body.",500)
		return
	}

	fmt.Printf("Body %s\n",body)
	json.Unmarshal(body, &m)
	mw.Content = m

	if mw.Content.Message == "" {
		http.Error(w,"Empty Message content field.", 500)
	}

	err = store.PostMsg(mw)
	if err != nil {
		http.Error(w,"Failed to post message.",500)
		return
	}
}

func GetMsgRange(w http.ResponseWriter, r *http.Request, id string, store MtsnStore) {
	ids := strings.Split(id, "-")
	top, err := strconv.Atoi(ids[0])
	bottom, err2 := strconv.Atoi(ids[1])
	if err != nil || err2 != nil {
		http.Error(w,"Invalid msg range",403)
		return
	}

	if bottom > top {
		http.Error(w,"Invalid msg range",403)
		return
	}

	delta := top - bottom
	if delta > 30 {
		http.Error(w,"Invalid msg range",403)
		return
	}

	msgs, err := store.GetMsgs(top, bottom)

	if err != nil {
		http.Error(w,"Failed to find messages.",500)
		return
	}

	msgBytes, _ := json.Marshal(msgs)
	w.Header().Set("Content-Type", "application/json")
	w.Write(msgBytes)
}

func GetOneMsg(w http.ResponseWriter, r *http.Request, ids string, store MtsnStore) {
	id, _ := strconv.Atoi(ids)

	mw, err := store.GetMsg(id)

	if err != nil {
		http.NotFound(w,r)
		return
	}

	w.Header().Set("Last-Modified", mw.Timestamp)
	msgBytes, _ := json.Marshal(mw.Content)
	w.Header().Set("Content-Type", "application/json")
	w.Write(msgBytes)
}

func GetMsg(w http.ResponseWriter, r *http.Request, ids string, store MtsnStore) {

	if strings.Contains(ids, "-") {
		GetMsgRange(w, r, ids, store)
	} else {
		GetOneMsg(w, r, ids, store)
	}

}


func GetLastMsg(w http.ResponseWriter, r *http.Request, store MtsnStore) {

	lastMsg, _ := store.GetLastMsg()
	b, _ := json.Marshal(lastMsg)
	w.Header().Set("Content-Type", "application/json")
	w.Write(b)
}
///////////////

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

func Unlock(lock chan int) {
	lock <- 0
}

//var session *mgo.Session

func main() {

	runtime.GOMAXPROCS(runtime.NumCPU())

	var (
		port   int
		ip     string
		db     string
		dbtype string
		servername string
	)
	flag.IntVar(&port, "port", 80, "Port to bind on.")
	flag.StringVar(&ip, "ip", "127.0.0.1", "IP to bind to")
	flag.StringVar(&db, "db", "muteswan", "MongoDB database to use")
	flag.StringVar(&dbtype, "dbtype", "mongo", "Database method to use, either mongo or file")
	flag.StringVar(&servername, "name", "defaultname", "Server name")
	flag.Parse()

	fmt.Print("Muteswan server\n")
	fmt.Printf("HTTP Port: %d\n", port)
	fmt.Printf("IP: %s\n", ip)
	fmt.Printf("DB name: %s\n", db)
	fmt.Printf("dbtype: %s\n", dbtype)
	fmt.Printf("Server name: %s\n", servername)


	//var session *mgo.Session
	var sess *mgo.Session

	if dbtype == "mongo" {
		session, err := mgo.Dial("localhost")
		if err != nil {
			panic(err)
		}
		defer session.Close()
		go ExpireMessageLoop(session.DB(db))
		session.SetMode(mgo.Monotonic, true)

		sess = session.Copy()
	}

	lock := make(chan int)
	go Unlock(lock)

	http.HandleFunc("/info", func(w http.ResponseWriter, r *http.Request) {
		si := &ServerInfo{Name: servername}
		infoBytes, _ := json.Marshal(si)
		w.Write(infoBytes)
	})

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		//dropPrivs(uid)


		var s *mgo.Session;
		if dbtype == "mongo" {
			s = sess.Copy()
			defer s.Close()
		}

		regex := regexp.MustCompile("^/(\\w{40}|\\w{64})/((\\d+-\\d+)|(\\d+))$")
		urlParts := regex.FindStringSubmatch(r.URL.Path)


		fmt.Printf("Got request %s\n",r.URL.Path)

		if urlParts == nil {
			regex2 := regexp.MustCompile("^/(\\w{40}|\\w{64})$")
			urlParts2 := regex2.FindStringSubmatch(r.URL.Path)

			if urlParts2 == nil {
				// return 404
				http.NotFound(w,r)
				return
			}
			mtsnStore := makeStore(dbtype,db,urlParts2[1],s,lock)

			if r.Method == "POST" {
				//fileStore := &FileStore{Circle: urlParts2[1], Datadir: db}
				PostMsg(w, r, mtsnStore)
			} else if r.Method == "GET" {
				//fileStore := &FileStore{Circle: urlParts2[1], Datadir: db}
				GetLastMsg(w, r, mtsnStore)
			}


		} else {
			//fileStore := &FileStore{Circle: urlParts[1], Datadir: db}
			mtsnStore := makeStore(dbtype,db,urlParts[1],s,lock)
			GetMsg(w, r, urlParts[2], mtsnStore)
		}


		//fmt.Printf("Got req: %s",urlParts[0])
		//fmt.Printf("Got hash: %s",urlParts[1])
		//fmt.Printf("Got ids: %s",urlParts[2])



	})

	httpd := &http.Server{
                Addr:           fmt.Sprintf("%s:%d",ip,port),
        }
	httpd.ListenAndServe()

}
