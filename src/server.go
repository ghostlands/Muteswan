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
	"io"
	"github.com/qpliu/qrencode-go/qrencode"
	"image/png"
 tiedot	"loveoneanother.at/tiedot/db"
	"errors"
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

type TiedotStore struct {
	Circle  string
	Db	*tiedot.DB
	Lock chan int
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


// tiedot implementation of MtsnStore
func (ts *TiedotStore) createDB() {
	_, err := os.Stat(ts.Db.Dir + "/" + ts.Circle)
	if err == nil {
		return
	}

	if os.IsNotExist(err) {
	  ts.Db.Create(ts.Circle)
	  circleCol := ts.Db.Use(ts.Circle)
	  circleCol.Index([]string{"Id"})
	  fmt.Printf("Created DB.")
	} else {
	  panic(fmt.Sprintf("Error creating DB dir: %s",err))
	}
}


func (ts *TiedotStore) updateCounter() int {
	ts.createDB()

	circleCol := ts.Db.Use(ts.Circle)

	result := make(map[uint64]bool)
	var query interface{}
	json.Unmarshal([]byte(`["=", {"eq": "message", "limit": 1, "in": ["last"]}]`), &query)
	if err := tiedot.EvalQuery(query, circleCol, &result); err != nil {
              panic(err)
        }

	if len(result) == 0 {
		fmt.Println("We got no last message object back - creating one.")
		//lm := TiedotLastMessage{LastMessage: 1, Last: "message"}
		//lm := LastMessage{LastMessage: 1}
		var lm interface{}
		json.Unmarshal([]byte(`{ "lastMessage": 1, "last": "message" }`),&lm)
		_, err := circleCol.Insert(lm)
		if err != nil {
			fmt.Printf("Failed to insert last message: %s\n", err)
			panic("Failed to insert last message.")
		}
		return 1
	} else {
		fmt.Println("Got last message object...")
		lm := &LastMessage{LastMessage: 0}
		var lastid uint64
		for lastid, _ = range result {
		    break
		}

		circleCol.Read(lastid,lm)

		var insrtLm interface{}
		lm.LastMessage = lm.LastMessage + 1
		jsonStr := fmt.Sprintf(`{ "lastMessage": %d, "last": "message" }`,lm.LastMessage)
		json.Unmarshal([]byte(jsonStr),&insrtLm)


		//lm.Last = "message"
		//lm.LastMessage = lm.LastMessage + 1
		_, err := circleCol.Update(lastid,insrtLm)
		if err != nil {
			fmt.Printf("Failed to update the last message record.")
			panic("Failed to update the last message record.")
		}
		fmt.Printf("Last message is: %d\n", lm.LastMessage)
		return lm.LastMessage
	}


}

func (ts *TiedotStore) PostMsg(msgw MsgWrap) error {
	ts.createDB()
	lock := <- ts.Lock

	if lock == 0 {
	  msgw.Id = ts.updateCounter()
	  msgw.Time = time.Now()
	  msgw.Timestamp = msgw.Time.UTC().Format(time.RFC1123)
	  msgw.Timestamp = strings.Replace(msgw.Timestamp, "UTC", "GMT", -1)
	  fmt.Printf("Received msgw: %v\n",msgw)


	  var insrtMsgw interface{}
	  bytes,err := json.Marshal(&msgw)
	  if err != nil {
		fmt.Printf("Failed to marshal: %s\n",err)
		return err
	  }
	  err = json.Unmarshal(bytes,&insrtMsgw)
	  if err != nil {
		fmt.Printf("Failed to unmarshal: %s\n",err)
		return err
	  }
	  fmt.Printf("arg: %v\n", insrtMsgw)

	  circleCol := ts.Db.Use(ts.Circle)
	  id,err := circleCol.Insert(insrtMsgw)
	  if err != nil {
		fmt.Printf("Failed to use tiedot db %s\n",ts.Circle)
		return err
	  }
	  fmt.Printf("Got new tiedot id: %d\n",id)

	  go Unlock(ts.Lock)
	}
	return nil
}

func (ts *TiedotStore) GetLastMsg() (LastMessage, error) {
	ts.createDB()
	circleCol := ts.Db.Use(ts.Circle)

	result := make(map[uint64]bool)
	var query interface{}
	json.Unmarshal([]byte(`["=", {"eq": "message", "limit": 1, "in": ["last"]}]`), &query)
	if err := tiedot.EvalQuery(query, circleCol, &result); err != nil {
              panic(err)
        }

	fmt.Println("Got last message object...")
	lm := &LastMessage{LastMessage: 0}
	var lastid uint64
	for lastid, _ = range result {
	    break
	}

	circleCol.Read(lastid,lm)
	fmt.Printf("Last message is: %d\n", lm.LastMessage)
	return *lm,nil

}

func (ts *TiedotStore) GetMsg(id int) (MsgWrap, error) {
	ts.createDB()
	circleCol := ts.Db.Use(ts.Circle)
	var msgw MsgWrap

	result := make(map[uint64]bool)
	var query interface{}
	queryStr := `["=", {"eq": ` + strconv.Itoa(id) + `, "limit": 1, "in": ["Id"]}]`
	fmt.Println("query string " + queryStr)
	json.Unmarshal([]byte(queryStr),&query)
	if err := tiedot.EvalQuery(query, circleCol, &result); err != nil {
		fmt.Printf("Error querying circle %d\n",id)
		return msgw,err
	}

	var mid uint64
	for mid,_ = range result {
		break
	}

	if mid == 0 {
		return msgw,errors.New("Could not find message id " + string(mid))
	}

	fmt.Printf("Returning mid %d\n", mid)
	err := circleCol.Read(mid,&msgw)
	if err != nil {
		fmt.Printf("Failed to read %d due to %s\n", mid, err)
		return msgw,err
	}
	fmt.Printf("Got message content: %v\n",msgw)

	return msgw,nil

}

func (ts *TiedotStore) GetMsgs(top int, bottom int) ([]MsgWrap, error) {

	var msgs []MsgWrap

	for i := top; i >= bottom; i-- {
		fmt.Printf("Fetching msg %d\n",i)
		mw,err := ts.GetMsg(i)
		if err != nil {
			return msgs,err
		}

		msgs = append(msgs,mw)
	}

	return msgs,nil

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
	path = fmt.Sprintf("%s%s%s%s%d", ms.Datadir, string(os.PathSeparator), ms.Circle, string(os.PathSeparator), id)
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


func validateHash(hash string) {
	matchSha1, _ := regexp.MatchString("^\\w{40}$", hash)
	matchSha256, _ := regexp.MatchString("^\\w{64}$", hash)
	if !matchSha1 || !matchSha256 {
		panic("Invalid hash circle key.")
	}
}

func makeStore( dbtype, db, hash string, s *mgo.Session, tdb *tiedot.DB, lock chan int) MtsnStore {


	if dbtype == "mongo" {
		return &MongoStore{Circle: hash, Db: s.DB(db)}
	}

	if dbtype == "file" {
		return &FileStore{Circle: hash, Datadir: db, Lock: lock}
	}

	if dbtype == "tiedot" {
		return &TiedotStore{Circle: hash, Db: tdb, Lock: lock}
	}

	return nil

}

// handler functions
////////////////////
func PostMsg(w http.ResponseWriter, r *http.Request, store MtsnStore) {
	var m Msg
	var mw MsgWrap

	body := make([]byte,r.ContentLength)
	_, err := io.ReadFull(r.Body,body)
	if err != nil {
		http.Error(w,"Error reading stream.",500)
		return
	}

	if len(body) == 0 {
		http.Error(w,"Empty body.",500)
		return
	}

	fmt.Printf("Body %s\n",body)
	err = json.Unmarshal(body, &m)
	if err != nil {
		http.Error(w,"Failed to parse JSON.", 500)
		return
	}
	mw.Content = m

	if mw.Content.Message == "" {
		http.Error(w,"Empty Message content field.", 500)
		return
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


func genQRCode(data string) (*qrencode.BitGrid,error) {
   bitgrid,err := qrencode.Encode(data,qrencode.ECLevelQ)
   if err != nil {
	return bitgrid,err
   }
   return bitgrid,nil
}

func ErrorMaxContent(w http.ResponseWriter) {
	http.Error(w,"Content too large.",413)
	return
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
	flag.IntVar(&port, "port", 8080, "Port to bind on.")
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


	// maximum conent length
	var maxContentLength int64
	maxContentLength = 16384

	//var session *mgo.Session
	var sess *mgo.Session
	var tdb *tiedot.DB

	if dbtype == "mongo" {
		session, err := mgo.Dial("localhost")
		if err != nil {
			panic(err)
		}
		defer session.Close()
		go ExpireMessageLoop(session.DB(db))
		session.SetMode(mgo.Monotonic, true)

		sess = session.Copy()
	} else if dbtype == "tiedot" {
		var err error
		tdb,err = tiedot.OpenDB(db)
		if err != nil {
			fmt.Printf("Failed to open tiedot db: %s\n",db)
		}
	}

	lock := make(chan int)
	go Unlock(lock)
	regex := regexp.MustCompile("^/(\\w{40}|\\w{64})/((\\d+-\\d+)|(\\d+))$")
	regex2 := regexp.MustCompile("^/(\\w{40}|\\w{64})$")

	http.HandleFunc("/info", func(w http.ResponseWriter, r *http.Request) {
		si := &ServerInfo{Name: servername}
		infoBytes, _ := json.Marshal(si)
		fmt.Println("Got server info", servername)
		w.Write(infoBytes)
	})

	http.HandleFunc("/qrcode", func(w http.ResponseWriter, r *http.Request) {
		qrcodeData,err := genQRCode(r.Host)
		if err != nil {
			http.Error(w,"Error generating QR code.",500)
			return
		}
		w.Header().Set("Content-Type", "image/png")
		png.Encode(w,qrcodeData.Image(6))

	})

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		//dropPrivs(uid)


		if r.ContentLength > maxContentLength {
			ErrorMaxContent(w)
			return
		}

		var s *mgo.Session;
		if dbtype == "mongo" {
			s = sess.Copy()
			defer s.Close()
		}

		urlParts := regex.FindStringSubmatch(r.URL.Path)


		//memstats := new(runtime.MemStats)
		//runtime.ReadMemStats(memstats)
		//fmt.Printf("Mem: %d\n", memstats.Alloc)
		fmt.Println("Got request",r.URL.Path)

		if urlParts == nil {
			urlParts2 := regex2.FindStringSubmatch(r.URL.Path)

			if urlParts2 == nil {
				// return 404
				http.NotFound(w,r)
				return
			}
			mtsnStore := makeStore(dbtype,db,urlParts2[1],s,tdb,lock)

			if r.Method == "POST" {
				//fileStore := &FileStore{Circle: urlParts2[1], Datadir: db}
				PostMsg(w, r, mtsnStore)
			} else if r.Method == "GET" {
				//fileStore := &FileStore{Circle: urlParts2[1], Datadir: db}
				GetLastMsg(w, r, mtsnStore)
			}


		} else {
			//fileStore := &FileStore{Circle: urlParts[1], Datadir: db}
			mtsnStore := makeStore(dbtype,db,urlParts[1],s,tdb,lock)
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
