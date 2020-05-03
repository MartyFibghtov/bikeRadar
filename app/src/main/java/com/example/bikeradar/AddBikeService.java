package com.example.bikeradar;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.DataQueryBuilder;
import com.example.bikeradar.activities.MainMenuActivity;
import com.example.bikeradar.classes.Bike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddBikeService extends IntentService {

    public AddBikeService() {
        super("AddBikeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(Constants.ACTION_ADD_EXISTING_BIKE)){
                String bikeId = intent.getStringExtra("bikeId");
                String userId = intent.getStringExtra("userId");
                Log.i("AddBikeService", "Service adding bike. UserObjectId: " + userId + " bikeId: " + bikeId);
                addExistingBikeToUser(userId, bikeId);
            }
        }

    }

    private void addExistingBikeToUser(String userId, String bikeId){
        HashMap<String, Object> parentObject = new HashMap<String, Object>();
        parentObject.put( "objectId", userId );

        HashMap<String, Object> childObject = new HashMap<String, Object>();
        childObject.put( "objectId", bikeId);

        ArrayList<Map> children = new ArrayList<Map>();
        children.add( childObject );

        Backendless.Data.of("Users").addRelation(parentObject, "bikes", children, new AsyncCallback<Integer>() {
            @Override
            public void handleResponse(Integer response) {
                Log.i("Adding bike", "Added successfully" );
                broadcastAddExistingBikeSuccess();
                //stopSelf();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                broadcastAddExistingBikeFailure();
                Log.i("Adding bike", fault.getMessage());
                //stopSelf();
            }
        });
    }

    private void broadcastAddExistingBikeSuccess(){
        Intent intent = new Intent(Constants.BROADCAST_ADD_BIKE_SUCCESS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        sendBroadcast(intent);
    }
    private void broadcastAddExistingBikeFailure(){
        Intent intent = new Intent(Constants.BROADCAST_ADD_BIKE_FAILURE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        sendBroadcast(intent);
    }

}
