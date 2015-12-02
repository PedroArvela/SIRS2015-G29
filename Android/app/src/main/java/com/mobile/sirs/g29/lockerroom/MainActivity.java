package com.mobile.sirs.g29.lockerroom;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    static final int CHALLANGE_ACTIVITY_CODE = 10;

    private TreeMap<String, Computer> _computers = new TreeMap<String, Computer>();
    private ArrayList<Computer> _comp;
    private ComputerListAdapter _computerAdapter;
    private ArrayList<String> _computerNameList = new ArrayList<String>();

    private int lastSelected = -1;
    private int challangeActivityReturn = 0; //0 == no return code, 1 == success, -1 == fail or return

    private void updateComputerList(){
        RemoteComunication rc = new RemoteComunication();
        //AsyncTask snt = new ScanNetworkTask();

        try{
            AsyncTask<Integer, String, ArrayList<String>> snt = new ScanNetworkTask((TextView)findViewById(R.id.scanProgressText), (Button)findViewById(R.id.testInsertionButton), _computerAdapter, _computers);

            Log.d("Scan-Network", "Executing task...");
            snt.execute(5000);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void revokeComputer(Computer c){
        //TODO: revoke computer session key
    }

    private void authorizeComputer(int selected){
        boolean action = false;
        Computer target = _comp.get(selected);
        lastSelected = selected;

        if(target.is_authorized()){
            target.set_authorized(false);
        } else {
            action = true;
        }

        if(action) {
            Intent challange = new Intent(this, ChallangeActivity.class);
            challange.putExtra("NAME", target.get_comptuerName());
            challange.putExtra("IP", target.get_computerIP());
            challange.putExtra("PORT", Integer.toString(target.get_port()));
            challange.putExtra("FINGERPRINT", target.get_fingerprint());
            startActivityForResult(challange, CHALLANGE_ACTIVITY_CODE);
            if(challangeActivityReturn == 1){
                //if challange was passed, update GUI and database
                Log.d("TARGET", "SET TRUE");
                target.set_authorized(true);
            }
            else if(challangeActivityReturn == -1){
                //challange was failed
                target.set_authorized(false);
                Log.d("TARGET", "SET FALSE");
            } else {
                Log.d("TARGET", "THIS SHOULD NOT HAPPEN");
            }
        }
        else{
            this.revokeComputer(target);
        }
        _computerAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == CHALLANGE_ACTIVITY_CODE) {
            Log.d("Challange->Main", "Returing...");
            Computer target = _comp.get(lastSelected);
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                challangeActivityReturn = 1;
                target.set_authorized(true);
                Log.d("Challange->Main", "Passed...");
            }
            if (resultCode == RESULT_CANCELED){
                challangeActivityReturn = -1;
                target.set_authorized(false);
                Log.d("Challange->Main", "Failed...");
            }
            _computerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _comp = new ArrayList<Computer>();

        ListView computerList = (ListView)findViewById(R.id.computerListView);
        _computerAdapter = new ComputerListAdapter(this, R.layout.computer_list_item, _comp);
        computerList.setAdapter(_computerAdapter);

        computerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                authorizeComputer(position);
            }
        });

        //TEST: inserts button
        final Button b = (Button)findViewById(R.id.testInsertionButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateComputerList();
            }
        });

    }
}
