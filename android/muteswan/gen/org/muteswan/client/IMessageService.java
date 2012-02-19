/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/rob/git/android/muteswan/src/org/muteswan/client/IMessageService.aidl
 */
package org.muteswan.client;
public interface IMessageService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.muteswan.client.IMessageService
{
private static final java.lang.String DESCRIPTOR = "org.muteswan.client.IMessageService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.muteswan.client.IMessageService interface,
 * generating a proxy if needed.
 */
public static org.muteswan.client.IMessageService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.muteswan.client.IMessageService))) {
return ((org.muteswan.client.IMessageService)iin);
}
return new org.muteswan.client.IMessageService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_refreshLatest:
{
data.enforceInterface(DESCRIPTOR);
this.refreshLatest();
reply.writeNoException();
return true;
}
case TRANSACTION_checkTorStatus:
{
data.enforceInterface(DESCRIPTOR);
org.muteswan.client.ITorVerifyResult _arg0;
_arg0 = org.muteswan.client.ITorVerifyResult.Stub.asInterface(data.readStrongBinder());
this.checkTorStatus(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isUserCheckingMessages:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isUserCheckingMessages();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setUserChecking:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setUserChecking(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getLastTorMsgId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _result = this.getLastTorMsgId(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_downloadMsgFromTor:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _result = this.downloadMsgFromTor(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_downloadLatestMsgRangeFromTor:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _result = this.downloadLatestMsgRangeFromTor(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_downloadMsgRangeFromTor:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
int _result = this.downloadMsgRangeFromTor(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_updateLastMessage:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
this.updateLastMessage(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_isPolling:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPolling();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_postMsg:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
int _result = this.postMsg(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.muteswan.client.IMessageService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void refreshLatest() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_refreshLatest, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void checkTorStatus(org.muteswan.client.ITorVerifyResult resultStatus) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((resultStatus!=null))?(resultStatus.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_checkTorStatus, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public boolean isUserCheckingMessages() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isUserCheckingMessages, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void setUserChecking(boolean checkValue) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((checkValue)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setUserChecking, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public int getLastTorMsgId(java.lang.String circleHash) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(circleHash);
mRemote.transact(Stub.TRANSACTION_getLastTorMsgId, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int downloadMsgFromTor(java.lang.String circleHash, int id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(circleHash);
_data.writeInt(id);
mRemote.transact(Stub.TRANSACTION_downloadMsgFromTor, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int downloadLatestMsgRangeFromTor(java.lang.String circleHash, int delta) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(circleHash);
_data.writeInt(delta);
mRemote.transact(Stub.TRANSACTION_downloadLatestMsgRangeFromTor, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int downloadMsgRangeFromTor(java.lang.String circleHash, int start, int last) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(circleHash);
_data.writeInt(start);
_data.writeInt(last);
mRemote.transact(Stub.TRANSACTION_downloadMsgRangeFromTor, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void updateLastMessage(java.lang.String circleHash, int lastMsg) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(circleHash);
_data.writeInt(lastMsg);
mRemote.transact(Stub.TRANSACTION_updateLastMessage, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public boolean isPolling() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPolling, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int postMsg(java.lang.String circleHash, java.lang.String msgContent) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(circleHash);
_data.writeString(msgContent);
mRemote.transact(Stub.TRANSACTION_postMsg, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_refreshLatest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_checkTorStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_isUserCheckingMessages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_setUserChecking = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getLastTorMsgId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_downloadMsgFromTor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_downloadLatestMsgRangeFromTor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_downloadMsgRangeFromTor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_updateLastMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_isPolling = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_postMsg = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
}
public void refreshLatest() throws android.os.RemoteException;
public void checkTorStatus(org.muteswan.client.ITorVerifyResult resultStatus) throws android.os.RemoteException;
public boolean isUserCheckingMessages() throws android.os.RemoteException;
public void setUserChecking(boolean checkValue) throws android.os.RemoteException;
public int getLastTorMsgId(java.lang.String circleHash) throws android.os.RemoteException;
public int downloadMsgFromTor(java.lang.String circleHash, int id) throws android.os.RemoteException;
public int downloadLatestMsgRangeFromTor(java.lang.String circleHash, int delta) throws android.os.RemoteException;
public int downloadMsgRangeFromTor(java.lang.String circleHash, int start, int last) throws android.os.RemoteException;
public void updateLastMessage(java.lang.String circleHash, int lastMsg) throws android.os.RemoteException;
public boolean isPolling() throws android.os.RemoteException;
public int postMsg(java.lang.String circleHash, java.lang.String msgContent) throws android.os.RemoteException;
}
