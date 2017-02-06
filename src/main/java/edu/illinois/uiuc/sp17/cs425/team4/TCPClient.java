package edu.illinois.uiuc.sp17.cs425.team4;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.StringCodec;
import edu.illinois.uiuc.sp17.cs425.team4.util.IOUtils;

class TCPClient
{
 public static void main(String argv[]) throws Exception
 {
  String sentence;
  String modifiedSentence;
  //BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
  Socket clientSocket = new Socket("localhost", 10005);
  DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
  //DataInput inFromServer = new DataInputStream(clientSocket.getInputStream());
  // sentence = inFromUser.readLine();
  sentence = "Hi I am awesome.";
  byte[] sentenceBytes = sentence.getBytes(StandardCharsets.UTF_8);
  outToServer.writeInt(sentenceBytes.length);
  outToServer.write(sentenceBytes);
  
  Codec<String> stringCodec = new StringCodec();
 /* byte[] serverResponse = IOUtils.readInputSizePrefixed(clientSocket.getInputStream()); 
  //modifiedSentence = inFromServer.readLine();
  //System.out.println("FROM SERVER: " + modifiedSentence);
  System.out.println(stringCodec.decode(serverResponse));*/
  clientSocket.close();
 }
}
