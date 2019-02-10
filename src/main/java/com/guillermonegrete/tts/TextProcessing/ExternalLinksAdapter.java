package com.guillermonegrete.tts.TextProcessing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.ExternalLink;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ExternalLinksAdapter extends RecyclerView.Adapter<ExternalLinksAdapter.ViewHolder> {

    private List<ExternalLink> links;
    private LayoutInflater inflater;

    public ExternalLinksAdapter(Context context, List<ExternalLink> links){
        this.links = links;
        inflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.external_link_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.linkButton.setText(links.get(position).siteName);
    }

    @Override
    public int getItemCount() {
        return links.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        Button linkButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            linkButton = itemView.findViewById(R.id.external_link_btn);
        }
    }
}
