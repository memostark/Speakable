package com.guillermonegrete.tts.TextProcessing;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.ExternalLink;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.RecyclerView;

public class ExternalLinksAdapter extends RecyclerView.Adapter<ExternalLinksAdapter.ViewHolder> {

    private List<ExternalLink> links;
    private String word;
    private LayoutInflater inflater;

    private Context context;

    public ExternalLinksAdapter(Context context, String word, List<ExternalLink> links){
        this.links = links;
        this.word = word;
        this.context = context;
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
        final String base_url = links.get(position).link;
        holder.linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(context, Uri.parse(
                        base_url.replace("{q}", word)
                ));
            }
        });
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
