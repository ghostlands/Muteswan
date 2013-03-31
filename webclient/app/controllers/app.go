package controllers

import (
	"github.com/robfig/revel"
	"github.com/hailiang/gosocks"
	"time"
	"crypto/sha1"
	"io"
	"io/ioutil"
	"fmt"
	"encoding/base64"
	"crypto/rand"
	"strings"
	"net/http"
	"encoding/json"
	"crypto/aes"
	"os"
	"crypto/cipher"
	"bufio"
	"errors"
	"bytes"
	"net/url"
)



/*** temporarily in here until I move to models ***/
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
        FullText string
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

func (circle *Circle) getFullText() string {
        if circle.Uuid == "" {
		return fmt.Sprintf("%s+%s@%s",circle.Shortname,circle.Key,circle.Server)
	} else {
		return fmt.Sprintf("%s+%s$%s@%s",circle.Shortname,circle.Uuid,circle.Key,circle.Server)
	}

	return ""
}

func (circle *Circle) SaveCircle() error {

	bytes,err := json.Marshal(circle)
	if err != nil {
		return(err)
	}

	// pull this from the revel config somehow FIXME
	dataDir := "/tmp/muteswan-client-data"
	circlesDir := dataDir + "/circles"
	err = ioutil.WriteFile(circlesDir + "/" + circle.getUrlHash(),bytes,0400)

	if err != nil {
		return(err)
	}

	return nil
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

func decryptMsgs (msgs []MsgWrap, circle *Circle) []MsgWrap {
	newMsgs := make([]MsgWrap,len(msgs))
	for i := range msgs {
		newMsgs[i] = msgs[i]
		fmt.Printf("Message id: %d\n", msgs[i].Id)
		newMsgs[i].Content.Message = msgs[i].Content.getPlaintextMessage(circle)
		fmt.Printf("Got message: %s\n",newMsgs[i].Content.Message)
	}
	return newMsgs
}

func (msg *Msg) getPlaintextMessage(circle *Circle) string {
	rawdata,_ := base64.StdEncoding.DecodeString(msg.Message)
	c,_ := aes.NewCipher(circle.getKeyData())
	decrypter := cipher.NewCBCDecrypter(c,msg.getIVData())
	plaintext := make([]byte,len(rawdata))

	fmt.Printf("rawdata: %s\n",msg.Message)
	fmt.Printf("plaintext len: %d\n", len(plaintext))
	fmt.Printf("rawdata len: %d\n", len(rawdata))
	fmt.Printf("iv data: %s\n", msg.getIVData())
	decrypter.CryptBlocks(plaintext,rawdata)
	return string(plaintext)
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


func pkcs5pad(data []byte, blocksize int) []byte {
        pad := blocksize - len(data)%blocksize
        b := make([]byte, pad, pad)
        for i := 0; i < pad; i++ {
                b[i] = uint8(pad)
        }
        return append(data, b...)
}


func newCircle(circleString string) (*Circle, error) {

        plusIndx := strings.Index(circleString,"+")
        sigilIndx := strings.Index(circleString,"$")
        atIndx := strings.Index(circleString,"@")

        var circle *Circle

        parsedCircle := []string{"","","",""}
        if plusIndx == -1 || atIndx == -1 {
                return circle,errors.New("Failed to parse circle string")
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

        circle = &Circle{Shortname: parsedCircle[0], Server: parsedCircle[3], Uuid: parsedCircle[1], Key: parsedCircle[2]}
	circle.FullText = circle.getFullText()
	fmt.Printf("We got a circle: %s\n", circle)
        return circle,nil
}





type MuteswanClient struct {
	*revel.Controller
}

func (c MuteswanClient) Index() revel.Result {
	return c.Render()
}

func (c MuteswanClient) PostMsg(circle, msgContent string) revel.Result {

        dialSocksProxy := socks.DialSocksProxy(socks.SOCKS4A, "127.0.0.1:9050")
        tr := &http.Transport{Dial: dialSocksProxy}
        httpClient := &http.Client{Transport: tr}

	mtsnCircle,err := newCircle(circle)
	if err != nil {
		return c.RenderError(err)
	}

	var msg Msg
	ciph,_ := aes.NewCipher(mtsnCircle.getKeyData())
	ivBytes := msg.genIVData()
        encrypter := cipher.NewCBCEncrypter(ciph, ivBytes)
	enctext := make([]byte, len(pkcs5pad([]byte(msgContent),16)))
	encrypter.CryptBlocks(enctext, pkcs5pad([]byte(msgContent),16))
	msg.Message = base64.StdEncoding.EncodeToString(enctext)
	msgBytes,_ := json.Marshal(msg)
	r := bytes.NewReader(msgBytes)
        resp,err := httpClient.Post(fmt.Sprintf("http://%s/%s",mtsnCircle.Server,mtsnCircle.getUrlHash()),"application/json",r)
	if err != nil {
		return c.RenderError(err)
	}

	if resp.StatusCode != 200 {
		return c.RenderError(err)
	}


	fmt.Printf("Redirect to Posts with: %s\n", mtsnCircle.FullText)
	return c.Redirect("/Posts?circle=%s",url.QueryEscape(mtsnCircle.FullText))
}

func (c MuteswanClient) AddCircle(circle string) revel.Result {

	if circle == "" {
		return c.Render()
	}

	mtsnCircle,err := newCircle(circle)
	if err != nil {
		return c.RenderError(err)
	}

	mtsnCircle.SaveCircle()

	return c.Redirect(MuteswanClient.CircleList)
}

func (c MuteswanClient) CircleList() revel.Result {

	// pull this from the revel config somehow FIXME
	dataDir := "/tmp/muteswan-client-data"
	circlesDir := dataDir + "/circles"
	msgsDir := dataDir + "/msgs"

	err1 := os.MkdirAll(circlesDir,0700)
	err2 := os.MkdirAll(msgsDir,0700)

	if err1 != nil {
		fmt.Printf("Error creating: %s\n", circlesDir)
		return c.RenderError(err1)
	}

	if err2 != nil {
		fmt.Printf("Error creating: %s\n", msgsDir)
		return c.RenderError(err2)
	}

	dir,err := os.Open(circlesDir)
	if err != nil {
		return c.RenderError(err)
	}


	fi,err := dir.Readdir(-1)
	if err != nil {
		fmt.Printf("Error reading from: %s\n", circlesDir)
		return c.RenderError(err)
		//return c.Render()
	}


	var circles []Circle
	for i := range fi {
		//circles = append(circles,fi[i].Name())
		path := circlesDir + "/" + fi[i].Name()
		fmt.Printf("Opening path %s\n", path)
		file,err := os.Open(path)
		if err != nil {
			fmt.Printf("Error reading from: %s\n", path)
			return c.RenderError(err)
		}
		bytes := ReadFileContents(file)

		var circle Circle
		err = json.Unmarshal(bytes,&circle)
		if err != nil {
			return c.RenderError(err)
		}

		// incase it wasn't there
		circle.FullText = circle.getFullText()
		circles = append(circles,circle)
	}


	return c.Render(circles)
}

func ReadFileContents(file *os.File) []byte {
        reader := bufio.NewReader(file)
        rawBytes, _ := ioutil.ReadAll(reader)
	return rawBytes
}


func (c MuteswanClient) Posts(circle string) revel.Result {


	if circle == "" {
		return c.Redirect(MuteswanClient.Index)
	}

        dialSocksProxy := socks.DialSocksProxy(socks.SOCKS4A, "127.0.0.1:9050")
        tr := &http.Transport{Dial: dialSocksProxy}
        httpClient := &http.Client{Transport: tr}


	mtsnCircle,err := newCircle(circle)
	if err != nil {
		return c.Render()
	}

	fmt.Printf("Got circle: %s\n", circle)

	r,err := httpClient.Get(fmt.Sprintf("http://%s/%s",mtsnCircle.Server,mtsnCircle.getUrlHash()))
	if err != nil {
                fmt.Printf("Error: %s\n",err)
		return c.Render()
	}

	bytes,err := ioutil.ReadAll(r.Body)
        if err != nil {
                fmt.Sprintf("Error: %s\n",err)
		return c.Render()
        }


	var msgs []MsgWrap
	var lowBound int
	lastMsg := &LastMessage{}
	json.Unmarshal(bytes,&lastMsg)
	if lastMsg.LastMessage >= 25 {
		lowBound = lastMsg.LastMessage - 25
	} else if lastMsg.LastMessage >= 1 {
		lowBound = lastMsg.LastMessage - (lastMsg.LastMessage - 1)
	}

	fmt.Printf("Lower bound: %d\n", lowBound)

	r,err = httpClient.Get(fmt.Sprintf("http://%s/%s/%d-%d",mtsnCircle.Server,mtsnCircle.getUrlHash(),lastMsg.LastMessage,lowBound))
        if err != nil {
                fmt.Sprintf("Error: %s\n",err)
		return c.Render()
        }

	bytes,err = ioutil.ReadAll(r.Body)
        if err != nil {
                fmt.Sprintf("Error: %s\n",err)
		return c.Render()
        }
	json.Unmarshal(bytes,&msgs)

	newMsgs := decryptMsgs(msgs,mtsnCircle)
	return c.Render(mtsnCircle,lastMsg,lowBound,newMsgs)
}

