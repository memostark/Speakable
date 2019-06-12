package com.guillermonegrete.tts.textprocessing;

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
    private String word;
    private LayoutInflater inflater;


    private DefaultWebBrowser defaultWebBrowser;

    ExternalLinksAdapter(Context context, String word, List<ExternalLink> links, DefaultWebBrowser defaultWebBrowser){
        this.links = links;
        this.word = word;
        this.defaultWebBrowser = defaultWebBrowser;
        inflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.external_link_item, parent, false);
        return new ViewHolder(view, defaultWebBrowser);
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

    class ViewHolder extends RecyclerView.ViewHolder{

        Button linkButton;
        String url;

        ViewHolder(@NonNull final View itemView, final DefaultWebBrowser defaultWebBrowser) {
            super(itemView);
            url = "";
            linkButton = itemView.findViewById(R.id.external_link_btn);
            linkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = itemView.getContext();
                    context.startActivity(defaultWebBrowser.intentForUrl(context, url));
                }
            });
        }
    }
}
