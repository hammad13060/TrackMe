package com.iiitd.hammad13060.trackme.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.iiitd.hammad13060.trackme.dbHandler.UsersDBHandler;
import com.iiitd.hammad13060.trackme.helpers.Constants;
import com.iiitd.hammad13060.trackme.helpers.Contact;
import com.iiitd.hammad13060.trackme.helpers.ContactsProvider;
import com.iiitd.hammad13060.trackme.helpers.JSONArrayRequest;
import com.iiitd.hammad13060.trackme.helpers.JSONRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ContactListUpdateService extends Service {

    private static String UPDATE_CONTACT_URL = Constants.SERVER_URL + "getContactList/";

    UpdateAppUsersRunnable contactListRunnable = null;
    Thread contactListThread = null;


    private static final String TAG = "ContactListService";
    List<Contact> contactList = null;

    RequestQueue requestQueue = null;
    private Response.Listener<JSONObject> responseListener = null;
    private Response.ErrorListener errorListener = null;

    public ContactListUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service started");
        contactListRunnable = new UpdateAppUsersRunnable();
        contactListThread = new Thread(contactListRunnable);
        contactListThread.start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contactListThread.stop();
    }

    private class UpdateAppUsersRunnable implements Runnable {

        private boolean requestState= true;
        @Override
        public void run() {
            // Moves the current Thread into the background
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            requestQueue = Volley.newRequestQueue(getApplicationContext());
            initResponseListener();
            initErrorListener();

            requestState = true;

            while (true) {
                if (requestState) {
                    ContactsProvider contactsProvider = new ContactsProvider(getApplicationContext());
                    contactList = contactsProvider.getContacts();

                    JSONArray requestArray = new JSONArray();

                    for (Contact contact : contactList) {
                        JSONObject contactObject = contact.toJSON();
                        requestArray.put(contactObject);
                    }

                    makePost(requestArray);
                }
            }

        }

        private void makePost(JSONArray contacts) {


            requestState = false;

            JSONObject requestJson = new JSONObject();

            try {
                requestJson.put(Constants.JSON_CONTACTS, contacts);

                Log.d(TAG, "sent: " + requestJson.toString());

                JSONRequest requestObject = new JSONRequest(Request.Method.POST, UPDATE_CONTACT_URL, null,
                        responseListener, errorListener, requestJson);
                requestQueue.add(requestObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void initResponseListener() {
            responseListener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    UsersDBHandler usersDBHandler = new UsersDBHandler(getApplicationContext());

                    Log.d(TAG,"received: " + response.toString());

                    try {
                        JSONArray contacts = response.getJSONArray(Constants.JSON_CONTACTS);

                        for (int i = 0; i < contacts.length(); i++) {
                            JSONObject contactJSON = contacts.getJSONObject(i);

                            /*int id = contactJSON.getInt(Contact.CONTACT_JSON_ID);
                            String name = contactJSON.getString(Contact.CONTACT_JSON_NAME);
                            JSONArray numbers = contactJSON.getJSONArray(Contact.CONTACT_JSON_NUMBERS);

                            List<String> phoneList = new ArrayList<>();

                            for (int j = 0; j < numbers.length(); j++) {
                                phoneList.add(numbers.getString(j));
                            }


                            Contact contact = new Contact();
                            contact.id = id;
                            contact.name = name;
                            contact.phoneList = phoneList;*/

                            Contact contact = Contact.jsonToContact(contactJSON);

                            usersDBHandler.addUser(contact);

                        }

                        Log.d(TAG, usersDBHandler.getAllUsers().toString());
                        requestState = true;

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

            }

        private void initErrorListener() {
            errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    requestState = true;
                }
            };
        }

        }
}