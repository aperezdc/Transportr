/*    Liberario
 *    Copyright (C) 2013 Torsten Grote
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.liberario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshScrollView;

import de.grobox.liberario.R;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Trip.Individual;
import de.schildbach.pte.dto.Trip.Leg;
import de.schildbach.pte.dto.Trip.Public;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TripsActivity extends Activity {
	private QueryTripsResult trips;
	private Menu mMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trips);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		final TableLayout main = (TableLayout) findViewById(R.id.activity_trips);

		Intent intent = getIntent();
		trips = (QueryTripsResult) intent.getSerializableExtra("de.schildbach.pte.dto.QueryTripsResult");

		addTrips(main, trips.trips);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Set a listener to be invoked when the list should be refreshed.
		PullToRefreshScrollView pullToRefreshView = (PullToRefreshScrollView) findViewById(R.id.pull_to_refresh_trips);
		pullToRefreshView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
				Mode mode = refreshView.getCurrentMode();
				boolean later = true;
				if(mode == Mode.PULL_FROM_START) later = false;
				else if(mode == Mode.PULL_FROM_END) later = true;
				startGetMoreTrips(later);
			}
		});
	}

	public void startGetMoreTrips(boolean later) {
		(new AsyncQueryMoreTripsTask(this, trips.context, later)).execute();
	}


	private void addTrips(final TableLayout main, List<Trip> trips, boolean append) {
		if(trips != null) {
			// reverse order of trips if they should be prepended
			if(!append) {
				ArrayList<Trip> tempResults = new ArrayList<Trip>(trips);
				Collections.reverse(tempResults);
				trips = tempResults;
			}

			for(final Trip trip : trips) {
				TableRow row = (TableRow) LayoutInflater.from(this).inflate(R.layout.trip, null);
				HorizontalScrollView scroll = (HorizontalScrollView) LayoutInflater.from(this).inflate(R.layout.trip_details, null);
				TableLayout details = (TableLayout) scroll.findViewById(R.id.trip_details);

				// Locations
				TextView fromView = (TextView) row.findViewById(R.id.fromView);
				fromView.setText(trip.from.name);
				TextView toView = ((TextView) row.findViewById(R.id.toView));
				toView.setText(trip.to.name);

				// Times
				TextView departureTimeView = ((TextView) row.findViewById(R.id.departureTimeView));
				departureTimeView.setText(DateUtils.getTime(trip.getFirstDepartureTime()));
				TextView arrivalTimeView = ((TextView) row.findViewById(R.id.arrivalTimeView));
				arrivalTimeView.setText(DateUtils.getTime(trip.getLastArrivalTime()));

				// Duration
				TextView durationView = ((TextView) row.findViewById(R.id.durationView));
				durationView.setText(DateUtils.getDuration(trip.getFirstDepartureTime(), trip.getLastArrivalTime()));

				// Transports
				TextView transportsView = ((TextView) row.findViewById(R.id.transportsView));
				transportsView.setText("");

				// Legs (Parts of the Trip)
				int i = 1;
				TableRow legViewOld = (TableRow) LayoutInflater.from(this).inflate(R.layout.trip_details_row, null);
				TableRow legViewNew;

				// for each leg
				for(final Leg leg : trip.legs) {
					legViewNew = (TableRow) LayoutInflater.from(this).inflate(R.layout.trip_details_row, null);

					TextView dDepartureViewNew = ((TextView) legViewNew.findViewById(R.id.dDepartureView));

					// only for the first leg
					if(i == 1) {
						// hide arrival time for start location of trip
						((LinearLayout) legViewOld.findViewById(R.id.dArrivialLinearLayout)).setVisibility(View.GONE);
						// only add old view the first time, because it isn't old there
						details.addView(legViewOld);
					}
					// only for the last leg
					if(i >= trip.legs.size()) {
						// span last row 
						TableRow.LayoutParams params = (TableRow.LayoutParams) dDepartureViewNew.getLayoutParams();
						params.span = 3;
						dDepartureViewNew.setLayoutParams(params);

						// hide stuff for last stop (destination)
						((LinearLayout) legViewNew.findViewById(R.id.dDepartureLinearLayout)).setVisibility(View.GONE);
						((TextView) legViewNew.findViewById(R.id.dDestinationView)).setVisibility(View.GONE);
						((TextView) legViewNew.findViewById(R.id.dLineView)).setVisibility(View.GONE);
						((TextView) legViewNew.findViewById(R.id.dMessageView)).setVisibility(View.GONE);
					}

					if(leg instanceof Trip.Public) {
						Public public_line = ((Public) leg);
						transportsView.setText(transportsView.getText() + " " + ((Public) leg).line.label.substring(0, 1));
						((TextView) legViewOld.findViewById(R.id.dDepartureTimeView)).setText(DateUtils.getTime(public_line.departureStop.getDepartureTime()));
						// TODO public_line.getDepartureDelay()
						((TextView) legViewOld.findViewById(R.id.dDepartureView)).setText(public_line.departureStop.location.name);
						((TextView) legViewOld.findViewById(R.id.dLineView)).setText(public_line.line.label.substring(1, public_line.line.label.length()));
						if(public_line.destination != null) {
							((TextView) legViewOld.findViewById(R.id.dDestinationView)).setText(public_line.destination.name);
						}
						((TextView) legViewNew.findViewById(R.id.dArrivalTimeView)).setText(DateUtils.getTime(public_line.arrivalStop.getArrivalTime()));
						// TODO public_line.getArrivalDelay()
						dDepartureViewNew.setText(public_line.arrivalStop.location.name);
						if(public_line.message == null) {
							((TextView) legViewOld.findViewById(R.id.dMessageView)).setVisibility(View.GONE);
						} else {
							((TextView) legViewOld.findViewById(R.id.dMessageView)).setVisibility(View.VISIBLE);
							((TextView) legViewOld.findViewById(R.id.dMessageView)).setText(public_line.message);
						}
						// get and add intermediate stops
						details.addView(getStops(public_line.intermediateStops));

						// make intermediate stops fold out and in on click
						legViewOld.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								ViewGroup parent = ((ViewGroup) view.getParent());
								View v = parent.getChildAt(parent.indexOfChild(view) + 1);
								if(v != null) {
									if(v.getVisibility() == View.GONE) {
										v.setVisibility(View.VISIBLE);
									}
									else if(v.getVisibility() == View.VISIBLE) {
										v.setVisibility(View.GONE);
									}
								}
							}

						});
					}
					else if(leg instanceof Trip.Individual) {
						transportsView.setText(transportsView.getText() + " W");
						Individual individual = (Trip.Individual) leg;

						((TextView) legViewOld.findViewById(R.id.dDepartureView)).setText(individual.departure.name);
						((TextView) legViewOld.findViewById(R.id.dDepartureTimeView)).setText(DateUtils.getTime(leg.departureTime));
						((TextView) legViewOld.findViewById(R.id.dLineView)).setText("W");
						((TextView) legViewOld.findViewById(R.id.dDestinationView)).setText(Integer.toString(individual.min) + " min " + Integer.toString(individual.distance) + " m");
						((TextView) legViewNew.findViewById(R.id.dArrivalTimeView)).setText(DateUtils.getTime(leg.arrivalTime));
						((TextView) legViewNew.findViewById(R.id.dDestinationView)).setText(individual.arrival.name);
						dDepartureViewNew.setText(individual.arrival.name);
					}

					details.addView(legViewNew);

					// save new leg view for next run of the loop
					legViewOld = legViewNew;

					i += 1;
				}

				// make trip details fold out and in on click
				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						View v = ((ViewGroup) view.getParent()).getChildAt(((ViewGroup) main).indexOfChild(view)+1);
						if(v.getVisibility() == View.GONE) {
							v.setVisibility(View.VISIBLE);
						}
						else if(v.getVisibility() == View.VISIBLE) {
							v.setVisibility(View.GONE);
						}
					}

				});

				if(append) {
					main.addView(row);
					main.addView(scroll);
				}
				else {
					main.addView(row, 0);
					main.addView(scroll, 1);
				}
			}
		}
		else {
			// TODO offer option to query again for trips
		}
	}

	private void addTrips(final TableLayout main, List<Trip> trips) {
		addTrips(main, trips, true);
	}

	private TableLayout getStops(List<Stop> stops) {
		TableLayout stopsView = new TableLayout(this);
		stopsView.setVisibility(View.GONE);

		if(stops != null) {
			for(final Stop stop : stops) {
				TableRow stopView = (TableRow) LayoutInflater.from(this).inflate(R.layout.stop, null);
				Date arrivalTime = stop.getArrivalTime();
				Date departureTime = stop.getDepartureTime();

				if(arrivalTime != null) {
					((TextView) stopView.findViewById(R.id.sArrivalTimeView)).setText(DateUtils.getTime(arrivalTime));
				}
				else {
					((TextView) stopView.findViewById(R.id.sArrivalTimeView)).setVisibility(View.GONE);
				}
				if(departureTime != null) {
					((TextView) stopView.findViewById(R.id.sDepartureTimeView)).setText(DateUtils.getTime(departureTime));
				}
				else {
					((TextView) stopView.findViewById(R.id.sDepartureTimeView)).setVisibility(View.GONE);
				}
				((TextView) stopView.findViewById(R.id.sLocationView)).setText(stop.location.name);

				stopsView.addView(stopView);
			}
		}

		return stopsView;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO show later/earlier options only if provider has the capability

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trips_activity_actions, menu);
		mMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();

				return true;
			case R.id.action_earlier:
				setProgress(false, true);
				startGetMoreTrips(false);

				return true;
			case R.id.action_later:
				setProgress(true, true);
				startGetMoreTrips(true);

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void addMoreTrips(QueryTripsResult trip_results, boolean later, int num_trips) {
		if(trips != null) {
			TableLayout main = (TableLayout) findViewById(R.id.activity_trips);
			int num_old_trips = trips.trips.size();
			List<Trip> trips_res = new ArrayList<Trip>(trip_results.trips);

			// remove old trips for providers that still return them
			if(trips_res.size() >= num_old_trips + num_trips) {
				if(later) {
					// remove the #num_old_trips first trips
					for(int i = 0; i < num_old_trips; i = i+1) {
						trips_res.remove(0);
					}
				}
				else {
					// remove the #num_old_trips last trips
					for(int i = 0; i < num_old_trips; i = i+1) {
						trips_res.remove(trips_res.size()-1);
					}
				}
			}
			// save trip results to have context for next query
			trips = trip_results;

			addTrips(main, trips_res, later);
		}
	}

	public void setProgress(Boolean later, Boolean progress) {
		MenuItem mMenuButtonMoreTrips = mMenu.findItem((later) ? R.id.action_later : R.id.action_earlier);
		PullToRefreshScrollView pullToRefreshView = (PullToRefreshScrollView) findViewById(R.id.pull_to_refresh_trips);

		if(progress) {
			View mActionButtonProgress = getLayoutInflater().inflate(R.layout.actionbar_progress_actionview, null);

			mMenuButtonMoreTrips.setActionView(mActionButtonProgress);
		}
		else {
			mMenuButtonMoreTrips.setActionView(null);
			pullToRefreshView.onRefreshComplete();
		}
	}


}
