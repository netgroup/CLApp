/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/daniele/git/CLAppDroidAlpha/src/it/uniroma2/mobilecollaborationplatform/querymanager/QueryManagerOp.aidl
 */
package it.uniroma2.mobilecollaborationplatform.querymanager;
public interface QueryManagerOp extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp
{
private static final java.lang.String DESCRIPTOR = "it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp interface,
 * generating a proxy if needed.
 */
public static it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp))) {
return ((it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp)iin);
}
return new it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
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
case TRANSACTION_getQueryResponse:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getQueryResponse(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getFile:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getFile(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements it.uniroma2.mobilecollaborationplatform.querymanager.QueryManagerOp
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public java.lang.String getQueryResponse(java.lang.String query) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(query);
mRemote.transact(Stub.TRANSACTION_getQueryResponse, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getFile(java.lang.String digest) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(digest);
mRemote.transact(Stub.TRANSACTION_getFile, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getQueryResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public java.lang.String getQueryResponse(java.lang.String query) throws android.os.RemoteException;
public java.lang.String getFile(java.lang.String digest) throws android.os.RemoteException;
}