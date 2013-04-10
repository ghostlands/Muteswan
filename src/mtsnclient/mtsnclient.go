package mtsnclient

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/ioutil"
	"strings"
	"time"
)

// A Muteswan message. This contains base64 encoded IV data
// and the message data (base64) encoded. The DecryptMsgs will
// return plaintext in the Message. Maybe this isn't a good idea?
type Msg struct {
	Iv      string `json:"iv,omitempty"`
	Message string `json:"message"`
}

// A wrapper of the messages used to provide metadata such as
// id and date information.
type MsgWrap struct {
	Content   Msg       `json:"content"`
	Timestamp string    `json:"timestamp"`
	Time      time.Time `json:"-"`
	Id        int       `bson:"_id"`
}

// Representation of the Muteswan server's lastMessage response.
type LastMessage struct {
	LastMessage int `json:"lastMessage"`
}

// A Muteswan Circle. This is the basic data structure for using Muteswan.
// Name, uuid, key and shortname are required to exchange or serialize the
// Circle information. The key is base64 encoded unless it is 16 unicode chars
// in which it is used as a literal AES key. The UUID may be ommitted to save
// space at the expense of security (a SHA1 hash of the key will be used in the
// URL).
type Circle struct {
	Shortname  string
	Server     string
	Uuid       string
	Key        string
	FullText   string
	rawKeyData []byte
}

// Returns the a SHA1 hash of the UUID (or key for circles which do not have a
// UUID) which is used in the URL to reference messages.
func (circle *Circle) GetUrlHash() string {
	h := sha1.New()
	if circle.Uuid == "" {
		io.WriteString(h, circle.Key)
	} else {
		io.WriteString(h, circle.Uuid)
	}
	return (fmt.Sprintf("%x", h.Sum(nil)))
}


// Returns a string representing the circle in standard form:
// name+uuid$key@server
func (circle *Circle) GetFullText() string {
	if circle.Uuid == "" {
		return fmt.Sprintf("%s+%s@%s", circle.Shortname, circle.Key, circle.Server)
	} else {
		return fmt.Sprintf("%s+%s$%s@%s", circle.Shortname, circle.Uuid, circle.Key, circle.Server)
	}

	return ""
}

// Saves circle data to the hard coded path in this method. What should we
// really do here?
func (circle *Circle) SaveCircle() error {

	bytes, err := json.Marshal(circle)
	if err != nil {
		return (err)
	}

	// pull this from the revel config somehow FIXME
	dataDir := "/tmp/muteswan-client-data"
	circlesDir := dataDir + "/circles"
	err = ioutil.WriteFile(circlesDir+"/"+circle.GetUrlHash(), bytes, 0400)

	if err != nil {
		return (err)
	}

	return nil
}


// Gets the raw bytes from the base64 key data.
func (circle *Circle) GetKeyData() []byte {
	if len(circle.rawKeyData) != 0 {
		return circle.rawKeyData
	}

	var keyData []byte
	if len(circle.Key) != 16 {
		keyData, _ = base64.StdEncoding.DecodeString(circle.Key)
	} else {
		keyData = []byte(circle.Key)
	}

	circle.rawKeyData = keyData
	return keyData
}

// Decode and return the IV data bytes.
func (msg *Msg) GetIVData() []byte {
	var iv []byte
	if msg.Iv == "" {
		iv = []byte{'0', '1', '2', '3', '4', '5', '6', '7', '0', '1', '2', '3', '4', '5', '6', '7'}
		//iv = []byte("01234567012345678")
	} else {
		iv, _ = base64.StdEncoding.DecodeString(msg.Iv)
	}
	return iv
}

// Given a slice of messages return a new slice of messages with plaintext in
// the Message field.
func (circle *Circle) DecryptMsgs(msgs []MsgWrap) []MsgWrap {

	newMsgs := make([]MsgWrap, len(msgs))

	for i := range msgs {
		newMsgs[i] = msgs[i]
		fmt.Printf("Message id: %d\n", msgs[i].Id)
		newMsgs[i].Content.Message = msgs[i].Content.getPlaintextMessage(circle)
		fmt.Printf("Got message: %s\n", newMsgs[i].Content.Message)
	}
	return newMsgs
}

// Decodes the encrypted data in the Msg and returns the plaintext as a string.
func (msg *Msg) getPlaintextMessage(circle *Circle) string {
	defer func() {
		if r := recover(); r != nil {
			fmt.Println("Failed to decrypt: ", r)
		}
	}()
	rawdata, _ := base64.StdEncoding.DecodeString(msg.Message)
	c, _ := aes.NewCipher(circle.GetKeyData())
	decrypter := cipher.NewCBCDecrypter(c, msg.GetIVData())
	plaintext := make([]byte, len(rawdata))

	fmt.Printf("rawdata: %s\n", msg.Message)
	fmt.Printf("plaintext len: %d\n", len(plaintext))
	fmt.Printf("rawdata len: %d\n", len(rawdata))
	fmt.Printf("iv data: %d\n", msg.GetIVData())
	decrypter.CryptBlocks(plaintext, rawdata)
	return string(plaintext)
}

// Generates 16 bytes of random data used for the IV
func (msg *Msg) GenIVData() []byte {
	rb := make([]byte, 16)
	_, err := rand.Read(rb)
	if err != nil {
		fmt.Printf("Failed to get random data: %s\n", err)
	}
	msg.Iv = base64.StdEncoding.EncodeToString(rb)
	return rb
}

// Utility function to pad data for AES
func Pkcs5pad(data []byte, blocksize int) []byte {
	pad := blocksize - len(data)%blocksize
	b := make([]byte, pad, pad)
	for i := 0; i < pad; i++ {
		b[i] = uint8(pad)
	}
	return append(data, b...)
}

// Initialize a circle structure using the standard Circle string format.
// Returns an error if it fails to parse.
func NewCircle(circleString string) (*Circle, error) {

	plusIndx := strings.Index(circleString, "+")
	sigilIndx := strings.Index(circleString, "$")
	atIndx := strings.Index(circleString, "@")

	var circle *Circle

	parsedCircle := []string{"", "", "", ""}
	if plusIndx == -1 || atIndx == -1 {
		return circle, errors.New("Failed to parse circle string")
	}

	if sigilIndx == -1 {
		parsedCircle[0] = circleString[0:plusIndx]
		parsedCircle[1] = ""
		parsedCircle[2] = circleString[plusIndx+1 : atIndx]
		parsedCircle[3] = circleString[atIndx+1 : len(circleString)]
	} else {
		parsedCircle[0] = circleString[0:plusIndx]
		parsedCircle[1] = circleString[plusIndx+1 : sigilIndx]
		parsedCircle[2] = circleString[sigilIndx+1 : atIndx]
		parsedCircle[3] = circleString[atIndx+1 : len(circleString)]
	}

	circle = &Circle{Shortname: parsedCircle[0], Server: parsedCircle[3], Uuid: parsedCircle[1], Key: parsedCircle[2]}
	circle.FullText = circle.GetFullText()
	fmt.Printf("We got a circle: %s\n", circle)
	return circle, nil
}
