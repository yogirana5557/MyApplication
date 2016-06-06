package com.demo.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Remote Config keys
    private static final String PRICE_CONFIG_KEY = "price";
    private static final String LOADING_PHRASE_CONFIG_KEY = "loading_phrase";
    private static final String PRICE_PREFIX_CONFIG_KEY = "price_prefix";
    private static final String DISCOUNT_CONFIG_KEY = "discount";
    private static final String IS_PROMOTION_CONFIG_KEY = "is_promotion_on";

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private TextView mPriceTextView;

    /**
     * The {@code FirebaseAnalytics} used to record screen views.
     */
    // [START declare_analytics]
    private FirebaseAnalytics mFirebaseAnalytics;
    // [END declare_analytics]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // [START shared_app_measurement]
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // [END shared_app_measurement]

        AppCompatButton signOutButton = (AppCompatButton) findViewById(R.id.signOutButton);
            signOutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hitClickEventAnalytics();

                    FirebaseAuth.getInstance().signOut();
                    finish();
                    startActivity(new Intent(MainActivity.this,LoginActivity.class));
                }
            });

        mPriceTextView = (TextView) findViewById(R.id.priceView);

        Button fetchButton = (Button) findViewById(R.id.fetchButton);
        fetchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hitClickEventAnalytics();

                fetchDiscount();
            }
        });

        // Get Remote Config instance.
        // [START get_remote_config_instance]
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        // [END get_remote_config_instance]

        // Create Remote Config Setting to enable developer mode.
        // Fetching configs from the server is normally limited to 5 requests per hour.
        // Enabling developer mode allows many more requests to be made per hour, so developers
        // can test different config values during development.
        // [START enable_dev_mode]
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        // [END enable_dev_mode]

        // Set default Remote Config values. In general you should have in app defaults for all
        // values that you may configure using Remote Config later on. The idea is that you
        // use the in app defaults and when you need to adjust those defaults, you set an updated
        // value in the App Manager console. Then the next time you application fetches from the
        // server, the updated value will be used. You can set defaults via an xml file like done
        // here or you can set defaults inline by using one of the other setDefaults methods.S
        // [START set_default_values]
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        // [END set_default_values]


        // Fetch discount config.
        fetchDiscount();


        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                String value = getIntent().getExtras().getString(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        // [END handle_data_extras]

        Button subscribeButton = (Button) findViewById(R.id.subscribeButton);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // [START subscribe_topics]
                hitClickEventAnalytics();

                FirebaseMessaging.getInstance().subscribeToTopic("news");
                Log.d(TAG, "Subscribed to news topic");
                // [END subscribe_topics]
            }
        });

        Button logTokenButton = (Button) findViewById(R.id.logTokenButton);
        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hitClickEventAnalytics();
                Log.d(TAG, "InstanceID token: " + FirebaseInstanceId.getInstance().getToken());
            }
        });


        // Checkbox to indicate when to catch the thrown exception.
        final CheckBox catchCrashCheckBox = (CheckBox) findViewById(R.id.catchCrashCheckBox);

        // Button that causes the NullPointerException to be thrown.
        Button crashButton = (Button) findViewById(R.id.crashButton);
        crashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Log that crash button was clicked. This version of Crash.log() will include the
                // message in the crash report as well as show the message in logcat.
                FirebaseCrash.logcat(Log.INFO, TAG, "Crash button clicked");

                // If catchCrashCheckBox is checked catch the exception and report is using
                // Crash.report(). Otherwise throw the exception and let Firebase Crash automatically
                // report the crash.
                if (catchCrashCheckBox.isChecked()) {
                    try {
                        throw new NullPointerException();
                    } catch (NullPointerException ex) {
                        // [START log_and_report]
                        FirebaseCrash.logcat(Log.ERROR, TAG, "NPE caught");
                        FirebaseCrash.report(ex);
                        // [END log_and_report]
                    }
                } else {
                    throw new NullPointerException();
                }
            }
        });

        // Log that the Activity was created. This version of Crash.log() will include the message
        // in the crash report but will not be shown in logcat.
        // [START log_event]
        FirebaseCrash.log("Activity created");
        // [END log_event]
    }

    private void hitClickEventAnalytics() {
        // [START image_view_event]
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Id");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Name");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "click");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        // [END image_view_event]
    }

    /**
     * Fetch discount from server.
     */
    private void fetchDiscount() {
        mPriceTextView.setText(mFirebaseRemoteConfig.getString(LOADING_PHRASE_CONFIG_KEY));

        long cacheExpiration = 3600; // 1 hour in seconds.
        // If in developer mode cacheExpiration is set to 0 so each fetch will retrieve values from
        // the server.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        // [START fetch_config_with_callback]
        // cacheExpirationSeconds is set to cacheExpiration here, indicating that any previously
        // fetched and cached config would be considered expired because it would have been fetched
        // more than cacheExpiration seconds ago. Thus the next fetch would go to the server unless
        // throttling is in progress. The default expiration duration is 43200 (12 hours).
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Fetch Succeeded");
                            // Once the config is successfully fetched it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            Log.d(TAG, "Fetch failed");
                        }
                        displayPrice();
                    }
                });
        // [END fetch_config_with_callback]
    }

    /**
     * Display price with discount applied if promotion is on. Otherwise display original price.
     */
    private void displayPrice() {
        long initialPrice = mFirebaseRemoteConfig.getLong(PRICE_CONFIG_KEY);
        long finalPrice = initialPrice;
        if (mFirebaseRemoteConfig.getBoolean(IS_PROMOTION_CONFIG_KEY)) {
            // [START get_config_values]
            finalPrice = initialPrice - mFirebaseRemoteConfig.getLong(DISCOUNT_CONFIG_KEY);
            // [END get_config_values]
        }
        mPriceTextView.setText(mFirebaseRemoteConfig.getString(PRICE_PREFIX_CONFIG_KEY) + finalPrice);
    }

}
