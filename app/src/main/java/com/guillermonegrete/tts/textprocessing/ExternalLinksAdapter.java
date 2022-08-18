package com.guillermonegrete.tts.textprocessing;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;

import com.guillermonegrete.tts.databinding.ExternalLinkFlatItemBinding;
import com.guillermonegrete.tts.databinding.ExternalLinkItemBinding;
import com.guillermonegrete.tts.db.ExternalLink;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ExternalLinksAdapter extends RecyclerView.Adapter<ExternalLinksAdapter.ViewHolder> {

    private final List<ExternalLink> links;
    private final String word;


    private final Callback callback;

    private boolean isFlat = false;

    public ExternalLinksAdapter(@NonNull String word, @NonNull List<ExternalLink> links, @NonNull Callback callback){
        this.links = links;
        this.word = word;
        this.callback = callback;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return isFlat ? new ViewHolder(ExternalLinkFlatItemBinding.inflate(inflater, parent, false), callback)
                : new ViewHolder(ExternalLinkItemBinding.inflate(inflater, parent, false), callback) ;
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

    /**
     * Set whether the button is using the flat layout.
     */
    public void setFlatButton(boolean isFlat) {
        this.isFlat = isFlat;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        Button linkButton;
        String url;

        ViewHolder(ExternalLinkItemBinding binding, Callback callback) {
            super(binding.getRoot());
            url = "";
            linkButton = binding.externalLinkBtn;
            linkButton.setOnClickListener(v -> callback.onClick(url));
        }

        ViewHolder(ExternalLinkFlatItemBinding binding, Callback callback) {
            super(binding.getRoot());
            url = "";
            linkButton = binding.externalLinkBtn;
            linkButton.setOnClickListener(v -> callback.onClick(url));
        }
    }

    public interface Callback{
        void onClick(String url);
    }
}
