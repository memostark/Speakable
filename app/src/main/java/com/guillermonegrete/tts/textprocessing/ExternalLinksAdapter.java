package com.guillermonegrete.tts.textprocessing;

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

    private final List<ExternalLink> links;
    private final String word;


    private final Callback callback;

    public ExternalLinksAdapter(String word, List<ExternalLink> links, Callback callback){
        this.links = links;
        this.word = word;
        this.callback = callback;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.external_link_item, parent, false);
        return new ViewHolder(view, callback);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.linkButton.setText(links.get(position).siteName);
        final String base_url = links.get(position).link;
        holder.url = base_url.replace("{q}", word);
    }

    @Override
    public int getItemCount() {
        return links.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        Button linkButton;
        String url;

        ViewHolder(@NonNull final View itemView, Callback callback) {
            super(itemView);
            url = "";
            linkButton = itemView.findViewById(R.id.external_link_btn);
            linkButton.setOnClickListener(v -> callback.onClick(url));
        }
    }

    public interface Callback{
        void onClick(String url);
    }
}
