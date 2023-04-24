package cn.edu.sustech.cs209.chatting.common;

import java.util.List;

public class PrivateMessage {

    private Long timestamp;

    public String sentBy;
    public String sentTo;


    private String data;

    public PrivateMessage(Long timestamp, String sentBy, String sentTo,List<String> data) {
      this.timestamp = timestamp;
      this.sentBy = sentBy;
      this.sentTo = sentTo;
      this.data="";
      StringBuilder strBuilder = new StringBuilder(this.data);
      for (String s:data){
        strBuilder.append(s).append("\n");
      }
      this.data = strBuilder.toString();
    }
    public String info(){
      String str="";
      StringBuilder strBuilder = new StringBuilder(str);
      strBuilder.append(timestamp);
      strBuilder.append('\n');
      strBuilder.append(sentBy);
      strBuilder.append('\n');
      strBuilder.append(sentTo);
      strBuilder.append('\n');
      strBuilder.append(data);
      strBuilder.append('\n');
      str=strBuilder.toString();
      return str;
    }

    public Long getTimestamp() {
      return timestamp;
    }

    public String getSentBy() {
      return sentBy;
    }

    public String getData() {
      return data;
    }
}
