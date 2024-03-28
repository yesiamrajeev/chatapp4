package com.example.chatapp4;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class MyAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private List<String> mData;

    public MyAdapter(Context context, List<String> data) {
        super(context, R.layout.list_item, data);
        mContext = context;
        mData = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        }

        TextView itemText = listItem.findViewById(R.id.item_text);
        itemText.setText(mData.get(position));

        return listItem;
    }
}
