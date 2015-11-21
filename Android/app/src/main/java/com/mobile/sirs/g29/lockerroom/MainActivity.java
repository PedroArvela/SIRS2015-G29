package com.mobile.sirs.g29.lockerroom;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    static final int CHALLANGE_ACTIVITY_CODE = 10;

    private TreeMap<String, Computer> _computers = new TreeMap<String, Computer>();
    private ArrayList<Computer> _comp;
    private ComputerListAdapter _computerAdapter;
    private ArrayList<String> _computerNameList = new ArrayList<String>();

    private int challangeActivityReturn = 0; //0 == no return code, 1 == success, -1 == fail or return

    private void updateComputerList(){
        String displayText;

        //TODO: reformulate method to actually fetch a valid list of computers in the network
        _computers.put("sith", new Computer("sith", "127.0.0.1", 1234));
        _computers.put("jedi", new Computer("jedi", "127.0.0.1", 1235));
        _computers.put("mandalorian", new Computer("mandalorian", "127.0.0.1", 1236));
        _computers.put("chiss", new Computer("chiss", "127.0.0.1", 1237));

        //Real code
        for(Computer comp : _computers.values()){
            if(!_computerAdapter.exists(comp)) {
                /*
                displayText = comp.get_comptuerName() + "\n" + comp.get_computerIP().toString();
                _computerNameList.add(displayText);
                */
                _computerAdapter.add(comp);
            }
        }
        _computerAdapter.notifyDataSetChanged();
    }

    private void revokeComputer(Computer c){
        //TODO: revoke computer session key
    }

    private void authorizeComputer(int selected){
        boolean action = false;
        Computer target = _comp.get(selected);

        if(target.is_authorized()){
            target.set_authorized(action);
        } else {
            action = true;
            target.set_authorized(action);
        }

        if(action) {
            Intent challange = new Intent(this, ChallangeActivity.class);
            challange.putExtra("NAME", target.get_comptuerName());
            challange.putExtra("IP", target.get_computerIP());
            challange.putExtra("PORT", Integer.toString(target.get_port()));
            startActivityForResult(challange, CHALLANGE_ACTIVITY_CODE);
            if(challangeActivityReturn == 1){
                //challange was passed, do nothing
            }
            else if(challangeActivityReturn == -1){
                //challange was failed
                target.set_authorized(false);
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
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                challangeActivityReturn = 1;
            }
            if (resultCode == RESULT_CANCELED){
                challangeActivityReturn = -1;
            }
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
