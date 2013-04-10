package main

import (
	"mtsnclient"
	"net/http"
	"github.com/hailiang/gosocks"
	"io/ioutil"
	"runtime"
	"fmt"
	"flag"
	"io"
	"encoding/json"
	"encoding/base64"
	"crypto/aes"
	"bytes"
	"os"
	"bufio"
	"crypto/cipher"
)

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

	circle,_ := mtsnclient.NewCircle(circleString)

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
		r,err = httpClient.Get(fmt.Sprintf("http://%s/%s",circle.Server,circle.GetUrlHash()))
	} else if cmdString == "read" {
		r,err = httpClient.Get(fmt.Sprintf("http://%s/%s/%s",circle.Server,circle.GetUrlHash(),arg1String))
		if err != nil {
			panic(fmt.Sprintf("Failed to download: %s\n", err))
		}
		lastModified := r.Header.Get("Last-Modified")
		fmt.Printf("Date: %s\n",lastModified)
	} else if cmdString == "post" {
		var msg mtsnclient.Msg

		c,_ := aes.NewCipher(circle.GetKeyData())
		ivBytes := msg.GenIVData()
		encrypter := cipher.NewCBCEncrypter(c, ivBytes)
		//fmt.Printf("Iv data is: %s\n",msg.Iv)

		//fmt.Printf("Key len: %d\n",len(circle.getKeyData()))
		//fmt.Printf("Iv len: %d\n",len(ivBytes))
		//fmt.Printf("Input len: %d\n",len([]byte(arg1String)))

		enctext := make([]byte, len(mtsnclient.Pkcs5pad([]byte(arg1String),16)))
		encrypter.CryptBlocks(enctext, mtsnclient.Pkcs5pad([]byte(arg1String),16))
		msg.Message = base64.StdEncoding.EncodeToString(enctext)
		msgBytes,_ := json.Marshal(msg)
		r := bytes.NewReader(msgBytes)
		resp,err := httpClient.Post(fmt.Sprintf("http://%s/%s",circle.Server,circle.GetUrlHash()),"application/json",r)
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

	var msg mtsnclient.Msg
	if cmdString == "read" {
		json.Unmarshal(bytes,&msg)

		//fmt.Printf("Msg: %s\n", msg.Message)
		//fmt.Printf("Iv: %s\n", msg.Iv)

		rawdata,_ := base64.StdEncoding.DecodeString(msg.Message)
		b := rawdata
		c,_ := aes.NewCipher(circle.GetKeyData())
		decrypter := cipher.NewCBCDecrypter(c, msg.GetIVData())
		plaintext := make([]byte, len(b))
		decrypter.CryptBlocks(plaintext, b)
		fmt.Printf("%s\n",string(plaintext))
	} else {
		fmt.Printf("%s\n",bytes)
	}
}
