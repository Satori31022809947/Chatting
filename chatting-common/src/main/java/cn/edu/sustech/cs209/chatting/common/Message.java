package cn.edu.sustech.cs209.chatting.common;

import java.util.ArrayList;
import java.util.List;

public class Message {

    private Long timestamp;

    private String sentBy;


    private String data;

    public Message(Long timestamp, String sentBy, List<String> data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.data="";
        StringBuilder strBuilder = new StringBuilder(this.data);
        for (String s:data){
            strBuilder.append(s+"\n");
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
