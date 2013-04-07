package controllers

import (
	"mtsnclient"
	"github.com/robfig/revel"
	"github.com/hailiang/gosocks"
	"io/ioutil"
	"fmt"
	"encoding/base64"
	"net/http"
	"encoding/json"
	"crypto/aes"
	"os"
	"crypto/cipher"
	"bufio"
	"errors"
	"bytes"
	"net/url"
	"github.com/qpliu/qrencode-go/qrencode"
	"image/png"
)


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

	mtsnCircle,err := mtsnclient.NewCircle(circle)
	if err != nil {
		return c.RenderError(err)
	}

	var msg mtsnclient.Msg
	ciph,_ := aes.NewCipher(mtsnCircle.GetKeyData())
	ivBytes := msg.GenIVData()
        encrypter := cipher.NewCBCEncrypter(ciph, ivBytes)
	enctext := make([]byte, len(mtsnclient.Pkcs5pad([]byte(msgContent),16)))
	encrypter.CryptBlocks(enctext, mtsnclient.Pkcs5pad([]byte(msgContent),16))
	msg.Message = base64.StdEncoding.EncodeToString(enctext)
	msgBytes,_ := json.Marshal(msg)
	r := bytes.NewReader(msgBytes)
        resp,err := httpClient.Post(fmt.Sprintf("http://%s/%s",mtsnCircle.Server,mtsnCircle.GetUrlHash()),"application/json",r)
	if err != nil {
		return c.RenderError(err)
	}

	if resp.StatusCode != 200 {
		return c.RenderError(err)
	}


	fmt.Printf("Redirect to Posts with: %s\n", mtsnCircle.FullText)
	return c.Redirect("/Posts?circle=%s",url.QueryEscape(mtsnCircle.FullText))
}

func (c MuteswanClient) JoinCircle(circle string) revel.Result {

	if circle == "" {
		return c.Render()
	}

	mtsnCircle,err := mtsnclient.NewCircle(circle)
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


	var circles []mtsnclient.Circle
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
		defer file.Close()

		var circle mtsnclient.Circle
		err = json.Unmarshal(bytes,&circle)
		if err != nil {
			return c.RenderError(err)
		}

		// incase it wasn't there
		circle.FullText = circle.GetFullText()
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


	mtsnCircle,err := mtsnclient.NewCircle(circle)
	if err != nil {
		return c.Render()
	}

	fmt.Printf("Got circle: %s\n", circle)
	bitgrid,err := qrencode.Encode(mtsnCircle.FullText,qrencode.ECLevelQ)
	if err != nil {
		fmt.Printf("Failed to encode qrcode: %s",err)
		return c.Render()
	}


	//FIXME: what do we do here for revel?
	goPath := os.Getenv("GOPATH")
	imageDir := goPath + "/src/muteswan-webclient/public/images"
	f,err := os.Create(imageDir + "/circle" + mtsnCircle.GetUrlHash() + ".png")
	if err != nil {
		return c.RenderError(errors.New("Could not render QR code."))
	}
	defer f.Close()
	png.Encode(f,bitgrid.Image(6))

	r,err := httpClient.Get(fmt.Sprintf("http://%s/%s",mtsnCircle.Server,mtsnCircle.GetUrlHash()))
	if err != nil {
                fmt.Printf("Error: %s\n",err)
		return c.Render()
	}

	bytes,err := ioutil.ReadAll(r.Body)
        if err != nil {
                fmt.Sprintf("Error: %s\n",err)
		return c.Render()
        }


	var msgs []mtsnclient.MsgWrap
	var lowBound int
	lastMsg := &mtsnclient.LastMessage{}
	json.Unmarshal(bytes,&lastMsg)
	if lastMsg.LastMessage >= 25 {
		lowBound = lastMsg.LastMessage - 25
	} else if lastMsg.LastMessage >= 1 {
		lowBound = lastMsg.LastMessage - (lastMsg.LastMessage - 1)
	}

	fmt.Printf("Lower bound: %d\n", lowBound)

	r,err = httpClient.Get(fmt.Sprintf("http://%s/%s/%d-%d",mtsnCircle.Server,mtsnCircle.GetUrlHash(),lastMsg.LastMessage,lowBound))
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

	newMsgs := mtsnclient.DecryptMsgs(msgs,mtsnCircle)
	urlHash := mtsnCircle.GetUrlHash()
	keylen := len(mtsnCircle.Key)
	rawkeylen := len(mtsnCircle.GetKeyData())
	return c.Render(mtsnCircle,lastMsg,lowBound,newMsgs,urlHash,keylen,rawkeylen)
}

