package main

import (
	"net/http"
	"github.com/hailiang/gosocks"
	"io/ioutil"
	"runtime"
	"fmt"
	"time"
	"flag"
	"strings"
	"crypto/sha1"
	"io"
	"encoding/json"
	"encoding/base64"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"bytes"
	"os"
	"bufio"
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

type Circle struct {
	Shortname string
	Server string
	Uuid string
	Key string
}

func (circle *Circle) getUrlHash() string {
	h := sha1.New()
	if circle.Uuid == "" {
		io.WriteString(h, circle.Key)
	} else {
		io.WriteString(h, circle.Uuid)
	}
	return(fmt.Sprintf("%x", h.Sum(nil)))
}

func (circle *Circle) getKeyData() []byte {
	var keyData []byte
	if len(circle.Key) != 16 {
		keyData,_ = base64.StdEncoding.DecodeString(circle.Key)
	} else {
		keyData = []byte(circle.Key)
	}

	return keyData
}

func (msg *Msg) getIVData() []byte {
	var iv []byte
	if msg.Iv == "" {
		iv = []byte{'0','1','2','3','4','5','6','7','0','1','2','3','4','5','6','7'}
	} else {
		iv,_ = base64.StdEncoding.DecodeString(msg.Iv)
	}
	return iv
}

func (msg *Msg) genIVData() []byte {
	rb := make([]byte,16)
	_,err := rand.Read(rb)
	if err != nil {
		fmt.Printf("Failed to get random data: %s\n", err)
	}
	msg.Iv = base64.StdEncoding.EncodeToString(rb)
	return rb
}

// only needed by AES?
func pkcs5pad(data []byte, blocksize int) []byte {
	pad := blocksize - len(data)%blocksize
	b := make([]byte, pad, pad)
	for i := 0; i < pad; i++ {
		b[i] = uint8(pad)
	}
	return append(data, b...)
}



func newCircle(circleString string) *Circle {
	plusIndx := strings.Index(circleString,"+")
	sigilIndx := strings.Index(circleString,"$")
	atIndx := strings.Index(circleString,"@")

	var circle *Circle

	parsedCircle := []string{"","","",""}
	if plusIndx == -1 || atIndx == -1 {
		return circle
	}

	if sigilIndx == -1 {
		parsedCircle[0] = circleString[0:plusIndx]
		parsedCircle[1] = ""
		parsedCircle[2] = circleString[plusIndx+1:atIndx]
		parsedCircle[3] = circleString[atIndx+1:len(circleString)]
	} else {
		parsedCircle[0] = circleString[0:plusIndx]
		parsedCircle[1] = circleString[plusIndx+1:sigilIndx]
		parsedCircle[2] = circleString[sigilIndx+1:atIndx]
		parsedCircle[3] = circleString[atIndx+1:len(circleString)]
	}

	//fmt.Printf("Key %s\n", parsedCircle[2])
	//fmt.Printf("Uuid %s\n", parsedCircle[1])
	circle = &Circle{Shortname: parsedCircle[0], Server: parsedCircle[3], Uuid: parsedCircle[1], Key: parsedCircle[2]}
	return circle
}


/*
func dumpData (one, two []byte) {
	for i,k := range two {
		fmt.Printf("%d: %b\n", i,k)
	}
	for i,k := range one {
		fmt.Printf("%d: %b\n", i,k)
	}
}
*/

func main() {

	runtime.GOMAXPROCS(runtime.NumCPU())

	var circleString string
	var cmdString string
	var arg1String string
	flag.StringVar(&circleString, "circle", "fortune+fb8eb78595536a4d@tckwndlytrphlpyo.onion", "The circle to use for cmds.")
	flag.StringVar(&cmdString, "cmd", "read", "The command to use.")
	flag.StringVar(&arg1String, "arg1", "1", "First argument")
	flag.Parse()

	//fmt.Printf("ARg1: %s\n", os.Args[0])

	dialSocksProxy := socks.DialSocksProxy(socks.SOCKS4A, "127.0.0.1:9050")
	tr := &http.Transport{Dial: dialSocksProxy}
	httpClient := &http.Client{Transport: tr}

	circle := newCircle(circleString)

	var err error
	var stdin io.Reader

	if arg1String == "-" {
	   stdin = bufio.NewReader(os.Stdin)
	   b,err := ioutil.ReadAll(stdin)
	   if err != nil {
		fmt.Printf("Failed to read stdin data.\n")
		return
	   }
	   //fmt.Printf("bytes %s\n",string(b))
	   arg1String = string(b)
	}

	var r *http.Response
	//fmt.Printf("Hash: %s\n", circle.getUrlHash())
	if cmdString == "last" {
		r,err = httpClient.Get(fmt.Sprintf("http://%s/%s",circle.Server,circle.getUrlHash()))
	} else if cmdString == "read" {
		r,err = httpClient.Get(fmt.Sprintf("http://%s/%s/%s",circle.Server,circle.getUrlHash(),arg1String))
		lastModified := r.Header.Get("Last-Modified")
		fmt.Printf("Date: %s\n",lastModified)
	} else if cmdString == "post" {
		var msg Msg

		c,_ := aes.NewCipher(circle.getKeyData())
		ivBytes := msg.genIVData()
		encrypter := cipher.NewCBCEncrypter(c, ivBytes)
		//fmt.Printf("Iv data is: %s\n",msg.Iv)

		//fmt.Printf("Key len: %d\n",len(circle.getKeyData()))
		//fmt.Printf("Iv len: %d\n",len(ivBytes))
		//fmt.Printf("Input len: %d\n",len([]byte(arg1String)))

		enctext := make([]byte, len(pkcs5pad([]byte(arg1String),16)))
		encrypter.CryptBlocks(enctext, pkcs5pad([]byte(arg1String),16))
		msg.Message = base64.StdEncoding.EncodeToString(enctext)
		msgBytes,_ := json.Marshal(msg)
		r := bytes.NewReader(msgBytes)
		resp,err := httpClient.Post(fmt.Sprintf("http://%s/%s",circle.Server,circle.getUrlHash()),"application/json",r)
		if err != nil {
			panic(fmt.Sprintf("Failed to post message: %s\n", err))
		}

		fmt.Printf("Response: %s\n", resp.Status)
		return

	}

	if err != nil {
		panic(fmt.Sprintf("Error: %s\n",err))
	}


	bytes,err := ioutil.ReadAll(r.Body)
	if err != nil {
		panic(fmt.Sprintf("Error: %s\n",err))
	}

	var msg Msg
	if cmdString == "read" {
		json.Unmarshal(bytes,&msg)

		//fmt.Printf("Msg: %s\n", msg.Message)
		//fmt.Printf("Iv: %s\n", msg.Iv)

		rawdata,_ := base64.StdEncoding.DecodeString(msg.Message)
		b := rawdata
		c,_ := aes.NewCipher(circle.getKeyData())
		decrypter := cipher.NewCBCDecrypter(c, msg.getIVData())
		plaintext := make([]byte, len(b))
		decrypter.CryptBlocks(plaintext, b)
		fmt.Printf("%s\n",string(plaintext))
	} else {
		fmt.Printf("%s\n",bytes)
	}
}
