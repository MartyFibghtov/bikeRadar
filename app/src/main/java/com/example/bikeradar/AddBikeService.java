package com.example.bikeradar;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.DataQueryBuilder;
import com.example.bikeradar.classes.Bike;

import java.util.List;


public class AddBikeService extends IntentService {

    public AddBikeService() {
        super("AddBikeService");
    }




    @Override
    protected void onHandleIntent(Intent intent) {
        int number = 6;
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(Constants.ACTION_ADD_BIKE)){
                String userName = intent.getStringExtra("userName");
                String bikeId = intent.getStringExtra("bikeId");
                Log.i("AddBikeService", "Service adding bike. Username: " + userName + " bikeId: " + bikeId);
                addBikeToUser(userName, bikeId);
            }
        }

    }

    private void addBikeToUser(String userName, final String bikeId){
        // find user

        final BackendlessUser[] users = new BackendlessUser[1];
        DataQueryBuilder queryUserBuilder = DataQueryBuilder.create();
        queryUserBuilder.setWhereClause(String.format("username= '%s'", userName));
        Backendless.Data.of(BackendlessUser.class).find(queryUserBuilder, new AsyncCallback<List<BackendlessUser>>() {
            @Override
            public void handleResponse(List<BackendlessUser> usersResponse) {
                if (usersResponse.size() == 1) {
                    users[0] = usersResponse.get(0);
                    Log.i("Found Users", users[0].getObjectId());
                } else {
                    // TODO more or less then one user
                }
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                // TODO error
                Log.e("Getting user", fault.getMessage());
            }
        });


        // find bike

        final Bike[] bikes = new Bike[1];
        Backendless.Data.mapTableToClass("bikes", Bike.class); // match table resp to class
        DataQueryBuilder queryBikeBuilder = DataQueryBuilder.create();;
        queryBikeBuilder.setWhereClause(String.format("objectId= '%s'", bikeId));
        Backendless.Data.of(Bike.class).find(queryBikeBuilder, new AsyncCallback<List<Bike>>() {
            @Override
            public void handleResponse(List<Bike> bikesResponse) {
                if (bikesResponse.size() == 1) {
                    bikes[0] = bikesResponse.get(0);
                    Log.i("Found Bike", bikes[0].name);
                } else {
                    // TODO more or less than one user
                }
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e("Getting bike", fault.getMessage());
            }
        });
        // получили велик и пользователя
        if (users[0] != null && bikes[0] != null){
            BackendlessUser user = users[0];
            Bike bike = bikes[0];
            // добавить пользователю велосипед
            updateBikesList(user, bike);
            Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                @Override
                public void handleResponse(BackendlessUser response) {

                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    // TODO error in updating 
                }
            });
        }


    }
    private void updateBikesList(BackendlessUser user, Bike bike){

    }


}
