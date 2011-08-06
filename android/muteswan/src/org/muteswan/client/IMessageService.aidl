package org.muteswan.client;

import org.muteswan.client.IMessageService;

interface IMessageService {

  void updateLastMessage();
  void downloadMessages();
  void longPoll();
  boolean isWorking();
  boolean torOnline();

}

