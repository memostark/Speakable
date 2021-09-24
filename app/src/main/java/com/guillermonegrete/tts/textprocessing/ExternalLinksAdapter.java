package com.guillermonegrete.tts.textprocessing;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.databinding.ExternalLinkItemBinding;
import com.guillermonegrete.tts.db.ExternalLink;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ExternalLinksAdapter extends RecyclerView.Adapter<ExternalLinksAdapter.ViewHolder> {

    private final List<ExternalLink> links;
    private final String word;


    private final Callback callback;

    private boolean isWrapped = false;

    public ExternalLinksAdapter(@NonNull String word, @NonNull List<ExternalLink> links, @NonNull Callback callback){
        this.links = links;
        this.word = word;
        this.callback = callback;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(ExternalLinkItemBinding.inflate(inflater, parent, false), isWrapped, callback);
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
     * Set if the width of thu button is wrapped instead of matching the parent.
     */
    public void setWrapped(boolean wrapped) {
        isWrapped = wrapped;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        Button linkButton;
        String url;

        ViewHolder(ExternalLinkItemBinding binding,  boolean isWrapped, Callback callback) {
            super(binding.getRoot());
            url = "";
            linkButton = itemView.findViewById(R.id.external_link_btn);
            linkButton = binding.externalLinkBtn;
            linkButton.setOnClickListener(v -> callback.onClick(url));
            if(isWrapped){
                ViewGroup.LayoutParams params = linkButton.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        }
    }

    public interface Callback{
        void onClick(String url);
    }
}
