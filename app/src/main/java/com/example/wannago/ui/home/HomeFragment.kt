package com.example.wannago.ui.home

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wannago.MainActivity
import com.example.wannago.R
import com.example.wannago.adapters.LocationAdapter
import com.example.wannago.databinding.FragmentHomeBinding
import com.example.wannago.helpers.ItemTouchHelperCallback
import com.example.wannago.model.Location
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.UUID

class HomeFragment() : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val markers = mutableListOf<Marker>()

    private lateinit var locationAdapter: LocationAdapter
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap
    private lateinit var geocoder: Geocoder

    private var locationArray: ArrayList<Location> = ArrayList()
    private var firestoreInstance : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = requireActivity()

        firestoreInstance = (activity as? MainActivity)?.getFirestoreInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        geocoder = Geocoder(requireContext(), Locale.getDefault())

        mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(onMapReadyCallback)

        binding.locationsRCV.layoutManager = LinearLayoutManager(context)

        locationAdapter = LocationAdapter(locationArray)
        binding.locationsRCV.adapter = locationAdapter

        retrieveFromLocationsCollection()

        val swipeToDeleteCallback = object : ItemTouchHelperCallback(){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                deleteFromLocationsCollection(locationArray.get(position))
                removeMarkerByPosition(locationArray.get(position))
                locationArray.removeAt(position)
                locationAdapter.notifyDataSetChanged()

            }

        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(binding.locationsRCV)

        val root: View = binding.root

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val onMapReadyCallback = OnMapReadyCallback { map ->
        googleMap = map

        val indianapolis = LatLng(39.76838000, -86.15804000)

        val initialCameraPosition = CameraPosition.Builder()
            .target(indianapolis)
            .zoom(12f)
            .build()

        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(initialCameraPosition))

        googleMap.setOnMapClickListener { location ->

            var locationName : String? = null
            val latitude = location.latitude
            val longitude = location.longitude

            val addresses: List<Address>? =
                try {
                    geocoder.getFromLocation(latitude, longitude, 1)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error getting address: ", e)
                    null
                }

            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    locationName = address.getAddressLine(0)

                    Log.d("HomeFragment", "Clicked location name: $locationName")

                    savetoLocationsCollection(Location(UUID.randomUUID().toString(), locationName, latitude, longitude))
                } else {
                    Log.w("HomeFragment", "No address found for clicked location")
                }
            }

            val markerToSet = MarkerOptions()
                .position(location)
                .title(locationName ?:"Place")  // Use retrieved name if available

            val marker = googleMap.addMarker(markerToSet)
            markers.add(marker!!)
        }
    }


    private fun savetoLocationsCollection(data: Location) {
        locationArray.add(data)
        locationAdapter.notifyDataSetChanged()

        val documentRef = firestoreInstance?.collection("locations")?.document(data.id)

        documentRef?.set(data)
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Data saved successfully!", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error saving data", Toast.LENGTH_SHORT).show()
            }

    }

    private fun deleteFromLocationsCollection(data: Location) {

        val documentRef = firestoreInstance?.collection("locations")?.document(data.id)

        documentRef?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Item Deleted successfully!", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error saving data", Toast.LENGTH_SHORT).show()
            }

    }

    private fun retrieveFromLocationsCollection() {

        firestoreInstance?.collection("locations")
            ?.get()
            ?.addOnSuccessListener { result ->
                for (document in result.documents) {
                    val data = document.toObject(Location::class.java)
                    if (data != null) {
                        locationArray.add(data)
                    }
                }
                locationAdapter.notifyDataSetChanged()

                for (location in locationArray) {
                    val markerOptions = MarkerOptions()
                        .position(LatLng(location.latitude, location.longitude))
                        .title(location.id)  // Or any other title you want

                    val marker = googleMap.addMarker(markerOptions)
                    markers.add(marker!!)

                }
            }
            ?.addOnFailureListener { exception ->
                Log.w("Firestore", "Error retrieving locations: ", exception)
            }
    }

    private fun removeMarkerByPosition(data: Location) {
        for (marker in markers) {
            if (marker.position.latitude == data.latitude && marker.position.longitude == data.longitude) {
                marker.remove()
                markers.remove(marker)
                break
            }
        }
    }

}