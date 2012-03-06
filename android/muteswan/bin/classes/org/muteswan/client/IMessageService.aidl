package org.muteswan.client;


import org.muteswan.client.IMessageService;
import org.muteswan.client.ITorVerifyResult;


interface IMessageService {

  void refreshLatest();
  void checkTorStatus(ITorVerifyResult resultStatus);
  boolean isSkipNextCheck();
  void setUserChecking(boolean checkValue);  
  
  
  int getLastTorMsgId(String circleHash);
  int downloadMsgFromTor(String circleHash, int id);
  int downloadLatestMsgRangeFromTor(String circleHash, int delta);
  int downloadMsgRangeFromTor(String circleHash, int start, int last);
  void updateLastMessage(String circleHash, int lastMsg);
  
  boolean isPolling();
  
  int postMsg(String circleHash, String msgContent); 
			
  
}

