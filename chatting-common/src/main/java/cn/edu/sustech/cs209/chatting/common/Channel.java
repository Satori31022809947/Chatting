package cn.edu.sustech.cs209.chatting.common;

import java.util.ArrayList;
import java.util.List;

public class Channel {
  public int id;
  public String name;
  public List<String> userList;
  public List<Message> messageList = new ArrayList<>();
  public Channel(List<String> userList,String name,int id){
    this.userList=userList;
    this.name=name;
    this.id=id;
  }
  public String info(){
    String str="";
    StringBuilder strBuilder = new StringBuilder(str);
    strBuilder.append(id);
    strBuilder.append('\n');
    for (String user:userList){
      strBuilder.append(user).append(" ");
    }
    strBuilder.append('\n');
    strBuilder.append(name);
    strBuilder.append('\n');
    str=strBuilder.toString();
    return str;
  }
  public void sendMessage(Message message){
    messageList.add(message);
  }
}
