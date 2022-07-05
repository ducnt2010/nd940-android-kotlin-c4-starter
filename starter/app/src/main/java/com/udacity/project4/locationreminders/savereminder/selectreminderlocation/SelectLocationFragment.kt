package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    private val REQUEST_LOCATION_PERMISSION = 1
    private val DEFAULT_LATITUDE = 37.422160
    private val DEFAULT_LONGTITUDE = -122.08123
    private val ZOOM_LEVEL = 15f

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private var currentMarker: Marker? = null

    // use to save selected location
    private var selectedLatLng: LatLng? = null
    private var selectedPoI: PointOfInterest? = null
    private var selectedAddress: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//         add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setSaveButtonEnable(false)
        binding.saveButton.setOnClickListener { onLocationSelected() }

        return binding.root
    }

    private fun onLocationSelected() {
        _viewModel.latitude.value = selectedLatLng?.latitude
        _viewModel.longitude.value = selectedLatLng?.longitude
        _viewModel.selectedPOI.value = selectedPoI
        _viewModel.reminderSelectedLocationStr.value=selectedAddress
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    private fun setSaveButtonEnable(enable: Boolean) {
        binding.saveButton.isEnabled = enable
        binding.saveButton.alpha = when (enable) {
            true -> 1f
            else -> 0.6f
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        //  Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val homeLatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGTITUDE)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, ZOOM_LEVEL))
//        map.addMarker(MarkerOptions().position(homeLatLng))

        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
        enableMyLocation()
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            resetCurrentMarker()
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            updateSelectedPosition(latLng, snippet)
            if(!binding.saveButton.isEnabled){
                setSaveButtonEnable(true)
            }
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            resetCurrentMarker()

            val poiMarker = map.addMarker(
                MarkerOptions().position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
            currentMarker = poiMarker

            updateSelectedPosition(poi.latLng, poi.name, poi)
            if(!binding.saveButton.isEnabled){
                setSaveButtonEnable(true)
            }
        }
    }

    private fun resetCurrentMarker() {
        if (currentMarker != null) {
            currentMarker?.remove()
            currentMarker = null
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "setMapStyle: Failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Cannot find style. Error ", e)
        }
    }

    private fun updateSelectedPosition(
        latLng: LatLng,
        address: String,
        poi: PointOfInterest? = null
    ) {
        selectedLatLng = latLng
        selectedAddress = address
        selectedPoI = poi
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.setMyLocationEnabled(true)
        } else {
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }
}
