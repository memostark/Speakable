package com.guillermonegrete.tts.textprocessing;

import android.view.LayoutInflater;
import android.view.View;
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

    private final Callback callback;

    private boolean isFlat = false;
    private int selectedPos = -1;

    public ExternalLinksAdapter(@NonNull List<ExternalLink> links, @NonNull Callback callback){
        this.links = links;
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
        holder.bind(links.get(position));
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

    public void setSelectedPos(int pos) {
        this.selectedPos = pos;
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        Button linkButton;
        View selectionView;

        ViewHolder(ExternalLinkItemBinding binding, Callback callback) {
            super(binding.getRoot());
            linkButton = binding.externalLinkBtn;
            linkButton.setOnClickListener(v -> callback.onClick(getAdapterPosition()));
        }

        ViewHolder(ExternalLinkFlatItemBinding binding, Callback callback) {
            super(binding.getRoot());
            linkButton = binding.externalLinkBtn;
            linkButton.setOnClickListener(v -> {
                callback.onClick(getAdapterPosition());
                notifyItemChanged(selectedPos);
                selectedPos = getAdapterPosition();
                notifyItemChanged(selectedPos);
            });
            selectionView = binding.bottomBorder;
        }

        void bind(ExternalLink link){
            linkButton.setText(link.siteName);
            if(selectionView != null) selectionView.setVisibility(getAdapterPosition() == selectedPos ? View.VISIBLE: View.GONE);
        }
    }

    public interface Callback{
        void onClick(int position);
    }
}
