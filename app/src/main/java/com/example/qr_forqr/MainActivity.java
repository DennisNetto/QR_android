package com.example.qr_forqr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;




// implements onClickListener for the onclick behaviour of button
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button scanBtn;
    TextView messageText, messageFormat, messageReturn;
    private Socket client;
    private PrintWriter printwriter;
    KeyPairGenerator kpg;
    KeyPair kp;
    PublicKey publicKey;
    PrivateKey privateKey;
    byte[] decryptedBytes;
    Cipher cipher1;
    String decrypted;




    class ClientThread implements Runnable {
        private String message;

        ClientThread(String message) {
            this.message = message;
            try {
                kpg = KeyPairGenerator.getInstance("RSA");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            kpg.initialize(2048);
            kp = kpg.genKeyPair();
            publicKey = kp.getPublic();
            privateKey = kp.getPrivate();
        }
        @Override
        public void run() {
            try {
                // the IP and port should be correct to have a connection established
                // Creates a stream socket and connects it to the specified port number on the named host.
                client = new Socket("18.219.232.254", 5050);  // connect to server
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                printwriter = new PrintWriter(client.getOutputStream(),true);
                System.out.println(publicKey);
                System.out.println(message);
                printwriter.write(message);  // write the message to output stream
                printwriter.flush();
                printwriter.write(String.valueOf(publicKey));
                printwriter.flush();

                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    String finalFromServer = fromServer;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println(finalFromServer);
                                byte[] serverty = org.apache.commons.codec.binary.Hex.decodeHex(finalFromServer);
                                cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                cipher1.init(Cipher.DECRYPT_MODE, privateKey);
                                decryptedBytes = cipher1.doFinal(serverty);
                                decrypted = new String(decryptedBytes);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println(decrypted);
                            messageReturn.setText(decrypted);


                        }
                    });

                    if (fromServer != null) {

                        break;
                    }
                }


                printwriter.close();
                in.close();


                // closing the connection
                client.close();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // referencing and initializing
        // the button and textviews
        scanBtn = findViewById(R.id.scanBtn);
        messageText = findViewById(R.id.textContent);
        messageFormat = findViewById(R.id.textFormat);
        messageReturn = findViewById(R.id.textReturn);

        // adding listener to the button
        scanBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        // we need to create the object
        // of IntentIntegrator class
        // which is the class of QR library
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setPrompt("Scan a barcode or QR Code");
        intentIntegrator.setOrientationLocked(true);
        intentIntegrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(getBaseContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // if the intentResult is not null we'll set
                // the content and format of scan message
                String text = intentResult.getContents();
                messageText.setText(text);
                messageFormat.setText(intentResult.getFormatName());
                new Thread(new ClientThread(text)).start();

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}