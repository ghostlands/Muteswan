package org.muteswan.client;


import org.muteswan.client.IMessageService;
import org.muteswan.client.ITorVerifyResult;


interface IMessageService {

  void refreshLatest();
  void checkTorStatus(ITorVerifyResult resultStatus);
  boolean isUserCheckingMessages();
  void setUserChecking(boolean checkValue);  
  
  
  int getLastTorMsgId(String circleHash);
  int downloadMsgFromTor(String circleHash, int id);
  void updateLastMessage(String circleHash, int lastMsg);
			
  
}

