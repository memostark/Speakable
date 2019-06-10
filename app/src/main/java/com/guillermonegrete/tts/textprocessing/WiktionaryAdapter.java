package com.guillermonegrete.tts.textprocessing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryLangHeader;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class WiktionaryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<WikiItem> items;

    private LayoutInflater inflater;

    public WiktionaryAdapter(Context context, List<WikiItem> items){
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.items = items;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == WikiItem.RowType.HEADER_ITEM.ordinal()){
            View view = inflater.inflate(R.layout.header_item, parent, false);
            return new HeaderViewHolder(view);
        }

        View view = inflater.inflate(R.layout.definition_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        WikiItem item = items.get(position);
        if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).setUp((WiktionaryItem) item);
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).setUp((WiktionaryLangHeader) item);
        }

    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemType().ordinal();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder{
        TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.textSeparator);
        }

        public void setUp(WiktionaryLangHeader item){
            headerText.setText(item.getLanguage());
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder{
        TextView subHeaderText;
        TextView definitionText;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            subHeaderText = itemView.findViewById(R.id.text_subheader);
            definitionText = itemView.findViewById(R.id.item_text);
        }

        public void setUp(WiktionaryItem item){
            subHeaderText.setText(item.getSubHeaderText());
            definitionText.setText(item.getItemText());
        }
    }
}
