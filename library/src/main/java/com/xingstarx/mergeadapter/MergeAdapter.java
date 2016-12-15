/***
 * Copyright (c) 2008-2009 CommonsWare, LLC
 * Portions (c) 2009 Google, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xingstarx.mergeadapter;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SectionIndexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter that merges multiple child adapters and views
 * into a single contiguous whole.
 * <p>
 * Adapters used as pieces within MergeAdapter must have
 * view type IDs monotonically increasing from 0. Ideally,
 * adapters also have distinct ranges for their row ids, as
 * returned by getItemId().
 */
public class MergeAdapter extends BaseAdapter implements SectionIndexer {
    protected PieceStateRoster pieces = new PieceStateRoster();

    /**
     * Stock constructor, simply chaining to the superclass.
     */
    public MergeAdapter() {
        super();
    }

    /**
     * Adds a new adapter to the roster of things to appear in
     * the aggregate list.
     *
     * @param adapter Source for row views for this section
     */
    public void addAdapter(ListAdapter adapter) {
        pieces.add(adapter);
        adapter.registerDataSetObserver(new CascadeDataSetObserver());
    }


    /**
     * Get the data item associated with the specified
     * position in the data set.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public Object getItem(int position) {
        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            int size = piece.getCount();

            if (position < size) {
                return piece.getItem(position);
            }

            position -= size;
        }

        return null;
    }

    /**
     * Get the adapter associated with the specified position
     * in the data set.
     *
     * @param position Position of the item whose adapter we want
     */
    public ListAdapter getAdapter(int position) {
        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            int size = piece.getCount();

            if (position < size) {
                return piece;
            }

            position -= size;
        }

        return null;
    }

    /**
     * How many items are in the data set represented by this
     * Adapter.
     */
    @Override
    public int getCount() {
        int total = 0;
        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            total += piece.getCount();
        }

        return total;
    }

    /**
     * Returns the number of types of Views that will be
     * created by getView().
     */
    @Override
    public int getViewTypeCount() {
        int total = 0;
        for (int i = 0 ; i < pieces.getRawPieces().size(); i++) {
            PieceState piece = pieces.getRawPieces().get(i);
            total += piece.adapter.getViewTypeCount();
        }

        return Math.max(total, 1); // needed for
        // setListAdapter() before
        // content add'
    }

    /**
     * Get the type of View that will be created by getView()
     * for the specified item.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public int getItemViewType(int position) {
        int typeOffset = 0;
        int result = -1;

        for (int i = 0 ; i < pieces.getRawPieces().size(); i++) {
            PieceState piece = pieces.getRawPieces().get(i);
            if (piece.isActive) {
                int size = piece.adapter.getCount();

                if (position < size) {
                    result = typeOffset + piece.adapter.getItemViewType(position);
                    break;
                }

                position -= size;
            }

            typeOffset += piece.adapter.getViewTypeCount();
        }

        return result;
    }

    /**
     * Are all items in this ListAdapter enabled? If yes it
     * means all items are selectable and clickable.
     */
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    /**
     * Returns true if the item at the specified position is
     * not a separator.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public boolean isEnabled(int position) {
        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            int size = piece.getCount();

            if (position < size) {
                return piece.isEnabled(position);
            }

            position -= size;
        }

        return false;
    }

    /**
     * get the specific Adapter to call it's onItemClick event
     */
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            int size = piece.getCount();
            if (position < size && piece instanceof AdapterView.OnItemClickListener) {
                ((AdapterView.OnItemClickListener) piece).onItemClick(adapterView, view, position, id);
                break;
            }
            position -= size;
        }
    }
    /**
     * Get a View that displays the data at the specified
     * position in the data set.
     *
     * @param position    Position of the item whose data we want
     * @param convertView View to recycle, if not null
     * @param parent      ViewGroup containing the returned View
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            int size = piece.getCount();

            if (position < size) {

                return piece.getView(position, convertView, parent);
            }

            position -= size;
        }

        return null;
    }

    /**
     * Get the row id associated with the specified position
     * in the list.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public long getItemId(int position) {
        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            int size = piece.getCount();

            if (position < size) {
                return piece.getItemId(position);
            }

            position -= size;
        }

        return -1;
    }

    @Override
    public int getPositionForSection(int section) {
        int position = 0;

        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            if (piece instanceof SectionIndexer) {
                Object[] sections = ((SectionIndexer) piece).getSections();
                int numSections = 0;

                if (sections != null) {
                    numSections = sections.length;
                }

                if (section < numSections) {
                    return position + ((SectionIndexer) piece).getPositionForSection(section);
                } else if (sections != null) {
                    section -= numSections;
                }
            }

            position += piece.getCount();
        }

        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        int section = 0;

        for (int i = 0 ; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            int size = piece.getCount();

            if (position < size) {
                if (piece instanceof SectionIndexer) {
                    return section + ((SectionIndexer) piece).getSectionForPosition(position);
                }

                return 0;
            } else {
                if (piece instanceof SectionIndexer) {
                    Object[] sections = ((SectionIndexer) piece).getSections();

                    if (sections != null) {
                        section += sections.length;
                    }
                }
            }

            position -= size;
        }

        return 0;
    }

    @Override
    public Object[] getSections() {
        ArrayList<Object> sections = new ArrayList<Object>();

        for (int i = 0; i < getPieces().size(); i++) {
            ListAdapter piece = getPieces().get(i);
            if (piece instanceof SectionIndexer) {
                Object[] curSections = ((SectionIndexer) piece).getSections();

                if (curSections != null) {
                    Collections.addAll(sections, curSections);
                }
            }
        }

        if (sections.size() == 0) {
            return (new String[0]);
        }

        return sections.toArray(new Object[0]);
    }

    public void setActive(ListAdapter adapter, boolean isActive) {
        pieces.setActive(adapter, isActive);
        notifyDataSetChanged();
    }

    public List<ListAdapter> getPieces() {
        return pieces.getPieces();
    }

    private static class PieceState {
        ListAdapter adapter;
        boolean isActive = true;

        PieceState(ListAdapter adapter, boolean isActive) {
            this.adapter = adapter;
            this.isActive = isActive;
        }
    }

    private static class PieceStateRoster {
        protected List<PieceState> pieces = new ArrayList<>();
        protected List<ListAdapter> active = null;

        void add(ListAdapter adapter) {
            pieces.add(new PieceState(adapter, true));
        }

        void setActive(ListAdapter adapter, boolean isActive) {
            for (int i = 0; i < pieces.size(); i++) {
                PieceState state = pieces.get(i);
                if (state.adapter == adapter) {
                    state.isActive = isActive;
                    active = null;
                    break;
                }
            }
        }

        List<PieceState> getRawPieces() {
            return (pieces);
        }

        List<ListAdapter> getPieces() {
            if (active == null) {
                active = new ArrayList<>();

                for (int i = 0; i < pieces.size(); i++) {
                    PieceState state = pieces.get(i);
                    if (state.isActive) {
                        active.add(state.adapter);
                    }
                }
            }

            return active;
        }
    }

    private class CascadeDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            notifyDataSetInvalidated();
        }
    }
}
