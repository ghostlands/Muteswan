/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/rob/git/android/muteswan/src/org/muteswan/client/ITorVerifyResult.aidl
 */
package org.muteswan.client;
public interface ITorVerifyResult extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.muteswan.client.ITorVerifyResult
{
private static final java.lang.String DESCRIPTOR = "org.muteswan.client.ITorVerifyResult";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.muteswan.client.ITorVerifyResult interface,
 * generating a proxy if needed.
 */
public static org.muteswan.client.ITorVerifyResult asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.muteswan.client.ITorVerifyResult))) {
return ((org.muteswan.client.ITorVerifyResult)iin);
}
return new org.muteswan.client.ITorVerifyResult.Stub.Proxy(obj);
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
case TRANSACTION_torFailure:
{
data.enforceInterface(DESCRIPTOR);
this.torFailure();
reply.writeNoException();
return true;
}
case TRANSACTION_torSuccess:
{
data.enforceInterface(DESCRIPTOR);
this.torSuccess();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.muteswan.client.ITorVerifyResult
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
public void torFailure() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_torFailure, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void torSuccess() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_torSuccess, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_torFailure = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_torSuccess = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void torFailure() throws android.os.RemoteException;
public void torSuccess() throws android.os.RemoteException;
}
