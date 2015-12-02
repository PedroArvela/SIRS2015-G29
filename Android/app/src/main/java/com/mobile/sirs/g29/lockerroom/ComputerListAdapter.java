package com.mobile.sirs.g29.lockerroom;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ComputerListAdapter extends ArrayAdapter<Computer>{

    Context _context;
    int _layoutResourceId;
    ArrayList<Computer> _computers = new ArrayList<Computer>();

    public ComputerListAdapter(Context context, int layoutResourceId, ArrayList<Computer> computerlist){
        super(context, layoutResourceId, computerlist);
        _context = context;
        _layoutResourceId = layoutResourceId;
        _computers = computerlist;
        _computers.clear();
    }

    public boolean exists(Computer c) {
        for (Computer temp : _computers) {
            if (temp.get_computerIP().equals(c.get_computerIP()) && temp.get_comptuerName().equals(c.get_comptuerName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void add(Computer c){
        _computers.add(c);
    }

    @Override
    public void remove(Computer c){
        _computers.remove(this.getPosition(c));
    }

    @Override
    public void clear(){
        _computers.clear();
    }

    @Override
    public int getCount() {
        return _computers.size();
    }

    @Override
    public int getPosition(Computer c){
        Computer temp;
        for(int i = 0; i < _computers.size(); i++){
            temp = _computers.get(i);
            if(temp.get_computerIP().equals(c.get_computerIP()) && temp.get_comptuerName().equals(c.get_comptuerName())){
                return i;
            }
        }

        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View row = convertView;
        ComputerHolder holder = null;

        if(row == null){
            LayoutInflater inflater = ((Activity)_context).getLayoutInflater();
            row = inflater.inflate(_layoutResourceId, parent, false);

            holder = new ComputerHolder();
            holder.authIcon = (ImageView)row.findViewById(R.id.computerItemAuthorization);
            holder.computerText = (TextView)row.findViewById(R.id.computerItemText);

            row.setTag(holder);
        }
        else{
            holder = (ComputerHolder)row.getTag();
        }

        Computer computer = _computers.get(position);
        String displayText = computer.get_comptuerName() + "\n" + computer.get_computerIP() + "\n" + computer.get_fingerprint();
        if(!computer.get_status().equals(Computer.STATUS.NONE)){
            displayText += "\n"+computer.get_status().toString();
        }
        holder.computerText.setText(displayText);
        holder.authIcon.setImageResource(computer.get_imageTag());

        return row;
    }

    static class ComputerHolder{
        ImageView authIcon;
        TextView computerText;
    }

}
