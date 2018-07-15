package com.microsoft.sdksample;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.TreeSet;

public class CustomAdapter extends BaseAdapter {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_SUBHEADER = 2;

    private ArrayList<String> mData = new ArrayList<>();
    private TreeSet<Integer> sectionHeader = new TreeSet<>();
    private TreeSet<Integer> sectionSubHeader = new TreeSet<>();

    private LayoutInflater mInflater;
    private int height;

    String TAG = this.getClass().getSimpleName();

    public CustomAdapter(Context context){
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        height=0;
    }

    public void addItem(final String item) {
        mData.add(item);
        notifyDataSetChanged();
    }

    public void addSectionHeaderItem(final String item) {
        mData.add(item);
        sectionHeader.add(mData.size() - 1);
        notifyDataSetChanged();
    }

    public void addSectionSubHeaderItem(final String item) {
        mData.add(item);
        sectionSubHeader.add(mData.size() - 1);
        notifyDataSetChanged();
    }

    public int getHeight() {return  height;}

    @Override
    public int getItemViewType(int position) {
        if(sectionHeader.contains(position)){
            return TYPE_SEPARATOR;
        }else if(sectionSubHeader.contains(position)){
            return TYPE_SUBHEADER;
        }
        return TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int rowType = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();
            switch (rowType) {
                case TYPE_ITEM:
                    convertView = mInflater.inflate(R.layout.definition_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.item_text);
                    break;
                case TYPE_SEPARATOR:
                    convertView = mInflater.inflate(R.layout.header_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.textSeparator);
                    break;
                case TYPE_SUBHEADER:
                    convertView = mInflater.inflate(R.layout.subheader_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.text_subheader);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView.setText(mData.get(position));
        return convertView;
    }

    public static class ViewHolder {
        public TextView textView;
    }
}
