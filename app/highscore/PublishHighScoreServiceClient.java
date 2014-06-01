package highscore;
import java.util.Iterator;
import java.util.UUID;

import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;

import play.Logger;

import org.w3c.dom.Node;

public class PublishHighScoreServiceClient {
    String firstName1;
    String firstName2;
    String lastName1;
    String lastName2;
    String birthdate1;
    String birthdate2;
    String gender1;
    String gender2;
    String status1;
    String status2;

    String operation = "HighScoreRequest";
    String urn = "http://big.tuwien.ac.at/we/highscore/data";
    String destination = "http://playground.big.tuwien.ac.at:8080/highscore/PublishHighScoreService";

    public PublishHighScoreServiceClient(String fn1, String ln1, String bd1, String g1, String s1, String fn2, String ln2, String bd2, String g2, String s2){
        firstName1 = fn1;
        firstName2 = fn2;
        lastName1 = ln1;
        lastName2 = ln2;
        birthdate1 = bd1;
        birthdate2 = bd2;
        gender1 = g1;
        gender2 = g2;
        status1 = s1;
        status2 = s2;
    }

    /*public static void main (String args[]) {
        PublishHighScoreServiceClient client = new PublishHighScoreServiceClient("gog","mar","1992-12-03","male","winner","lucy","walker","1994-07-10","female","loser");
        client.fire(); // !
    }*/

    public String fire(){
        try{
            // First create the connection
            SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnFactory.createConnection();

            // Next, create the actual message
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage message = messageFactory.createMessage();

            SOAPPart soapPart = message.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            envelope.addNamespaceDeclaration("data", urn);

            // Create and populate the body
            SOAPBody body = envelope.getBody();

            // Create the main element
            SOAPElement bodyElement = body.addChildElement(operation, "data");
            bodyElement.addChildElement("UserKey","data").addTextNode("rkf4394dwqp49x");
            SOAPElement quizElement = bodyElement.addChildElement("quiz");
            SOAPElement usersElement = quizElement.addChildElement("users");

            SOAPElement user1Element = usersElement.addChildElement("user");
            user1Element.addAttribute(envelope.createName("name"), status1);
            user1Element.addAttribute(envelope.createName("gender"), gender1);
            user1Element.addChildElement("password");
            user1Element.addChildElement("firstname").addTextNode(firstName1);
            user1Element.addChildElement("lastname").addTextNode(lastName1);
            user1Element.addChildElement("birthdate").addTextNode(birthdate1);

            SOAPElement user2Element = usersElement.addChildElement("user");
            user2Element.addAttribute(envelope.createName("name"), status2);
            user2Element.addAttribute(envelope.createName("gender"), gender2);
            user2Element.addChildElement("password");
            user2Element.addChildElement("firstname").addTextNode(firstName2);
            user2Element.addChildElement("lastname").addTextNode(lastName2);
            user2Element.addChildElement("birthdate").addTextNode(birthdate2);

            // Save the message
            message.saveChanges();

            // Print the message
            System.out.println("\nRequest:\n");
            message.writeTo(System.out);
            System.out.println();

            // Send the message and get the reply
            SOAPMessage reply = connection.call(message, destination);

            // Print the response
            System.out.println("\nResponse:\n");
            reply.writeTo(System.out);
            System.out.println();

            // Retrieve the result - no error checking is done: BAD!
            soapPart = reply.getSOAPPart();
            envelope = soapPart.getEnvelope();
            body = envelope.getBody();

            String uuid = null;
            Node target = body.getFirstChild();
            if(!target.getLocalName().equals("HighScoreResponse")){
                Logger.error("Bad request/response.");
            } else {
                uuid = target.getFirstChild().getNodeValue();
                Logger.info("\nUUID retrieved: " + uuid + "\n");
            }
            // Close the connection
            connection.close();

            if(uuid==null) {
                uuid = UUID.randomUUID().toString();
            }
            return uuid;

        } catch (Exception e) {

        }
        return null;
    }
}
