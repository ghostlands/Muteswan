package org.aftff.client;

import org.aftff.client.IMessageService;

interface IMessageService {

  void updateLastMessage();
  void downloadMessages();
  void poll();
  void longPoll();
  boolean isWorking();

}

