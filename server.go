package main

import (
	"bufio"
	"bytes"
	"code.google.com/p/goweb/goweb"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"strings"
	"strconv"
	"encoding/json"
	"time"
)


type Msg struct {
    Iv string `json:"iv"`
    Message string `json:"message"`
}

type MsgWrap struct {
    Content Msg `json:"content"`
    Timestamp string `json:"timestamp"`
}

//type LastMsg struct {
//	LastMessage int `json:"lastMessage"`
//}

func (m *Msg) getIv() string {
	return(m.Iv)
}

func (m *Msg) getMessage() string {
	return(m.Message)
}


// utility functions
func ReadFileContents(file *os.File) string {
	reader := bufio.NewReader(file)
	rawBytes, _ := ioutil.ReadAll(reader)
	buffer := bytes.NewBuffer(rawBytes)
	return (buffer.String())
}


func GetLatestMsg(hash string) int {
	path := fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s", hash)
	dir,_ := os.Open(path)
	files,_ := dir.Readdirnames(10000)
	var mostRecent int
	for i := range files {
		id,_ := strconv.Atoi(files[i])
		if id > mostRecent {
			mostRecent = id
		}

	}
	return(mostRecent)
}

func PostMsg(c *goweb.Context) {
	mostRecent := GetLatestMsg(c.PathParams["hash"])
	msgId := mostRecent+1
	filepath := fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s/%d",c.PathParams["hash"],msgId)
	file,_ := os.Create(filepath)
	wr := bufio.NewWriter(file)
	io.Copy(wr,c.Request.Body)
	wr.Flush()
	//fmt.Fprintf(c.ResponseWriter, "Wrote %d bytes.", written)
}

func GetMsg(c *goweb.Context) {

	if strings.Contains(c.PathParams["id"], "-") {

		ids := strings.Split(c.PathParams["id"],"-")
		top,_ := strconv.Atoi(ids[0])
		bottom,_ := strconv.Atoi(ids[1])
		if bottom > top {
			fmt.Fprintf(c.ResponseWriter, "wtf %d to %d make no sense", top, bottom)
			return;
		}

		var msgs []MsgWrap
		for i := top; i >= bottom; i-- {
			var m Msg
			var mw MsgWrap
			//var m interface{}
			path := fmt.Sprintf("/home/junger/gowebtest/muteswansrv-hidden/data/%s/%d", c.PathParams["hash"], i)
			file,err := os.Open(path)
			if err != nil {
			  fmt.Fprintf(c.ResponseWriter, "wtf not found %d", i)
			  return;
			}
			jsonString := ReadFileContents(file)
			bytes := []byte(jsonString)
			err = json.Unmarshal(bytes, &m)
			mw.Content = m

			stat, _ := file.Stat()
			mw.Timestamp = stat.ModTime().Format(time.RFC1123)
			msgs = append(msgs,mw)
		}

		msgBytes,_ := json.Marshal(msgs)
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

func GetLastMsg(c *goweb.Context) {

	mostRecent := GetLatestMsg(c.PathParams["hash"])

	// use interface and unmarshal?
	fmt.Fprintf(c.ResponseWriter, `{"lastMessage":"%d"}`, mostRecent)
}

func main() {


	goweb.MapFunc("/{hash}/{id}", func(c *goweb.Context) {
		fmt.Printf("URL: %s\n",c.Request.URL.String())
		GetMsg(c)
	})

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		fmt.Printf("URL: %s\n",c.Request.URL.String())
		GetLastMsg(c)
	}, goweb.GetMethod)

	goweb.MapFunc("/{hash}", func(c *goweb.Context) {
		fmt.Printf("URL: %s\n",c.Request.URL.String())
		PostMsg(c)
	}, goweb.PostMethod)

	goweb.ListenAndServe(":8081")

}
