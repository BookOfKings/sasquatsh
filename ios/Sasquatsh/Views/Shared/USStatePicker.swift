import SwiftUI

struct USStatePicker: View {
    @Binding var selection: String
    var label: String = "State"

    var body: some View {
        Picker(label, selection: $selection) {
            Text("Select").tag("")
            ForEach(USState.allStates, id: \.abbreviation) { state in
                Text("\(state.name)").tag(state.abbreviation)
            }
        }
    }
}

enum USState {
    struct State {
        let abbreviation: String
        let name: String
    }

    static let allStates: [State] = [
        State(abbreviation: "AL", name: "Alabama"),
        State(abbreviation: "AK", name: "Alaska"),
        State(abbreviation: "AZ", name: "Arizona"),
        State(abbreviation: "AR", name: "Arkansas"),
        State(abbreviation: "CA", name: "California"),
        State(abbreviation: "CO", name: "Colorado"),
        State(abbreviation: "CT", name: "Connecticut"),
        State(abbreviation: "DE", name: "Delaware"),
        State(abbreviation: "FL", name: "Florida"),
        State(abbreviation: "GA", name: "Georgia"),
        State(abbreviation: "HI", name: "Hawaii"),
        State(abbreviation: "ID", name: "Idaho"),
        State(abbreviation: "IL", name: "Illinois"),
        State(abbreviation: "IN", name: "Indiana"),
        State(abbreviation: "IA", name: "Iowa"),
        State(abbreviation: "KS", name: "Kansas"),
        State(abbreviation: "KY", name: "Kentucky"),
        State(abbreviation: "LA", name: "Louisiana"),
        State(abbreviation: "ME", name: "Maine"),
        State(abbreviation: "MD", name: "Maryland"),
        State(abbreviation: "MA", name: "Massachusetts"),
        State(abbreviation: "MI", name: "Michigan"),
        State(abbreviation: "MN", name: "Minnesota"),
        State(abbreviation: "MS", name: "Mississippi"),
        State(abbreviation: "MO", name: "Missouri"),
        State(abbreviation: "MT", name: "Montana"),
        State(abbreviation: "NE", name: "Nebraska"),
        State(abbreviation: "NV", name: "Nevada"),
        State(abbreviation: "NH", name: "New Hampshire"),
        State(abbreviation: "NJ", name: "New Jersey"),
        State(abbreviation: "NM", name: "New Mexico"),
        State(abbreviation: "NY", name: "New York"),
        State(abbreviation: "NC", name: "North Carolina"),
        State(abbreviation: "ND", name: "North Dakota"),
        State(abbreviation: "OH", name: "Ohio"),
        State(abbreviation: "OK", name: "Oklahoma"),
        State(abbreviation: "OR", name: "Oregon"),
        State(abbreviation: "PA", name: "Pennsylvania"),
        State(abbreviation: "RI", name: "Rhode Island"),
        State(abbreviation: "SC", name: "South Carolina"),
        State(abbreviation: "SD", name: "South Dakota"),
        State(abbreviation: "TN", name: "Tennessee"),
        State(abbreviation: "TX", name: "Texas"),
        State(abbreviation: "UT", name: "Utah"),
        State(abbreviation: "VT", name: "Vermont"),
        State(abbreviation: "VA", name: "Virginia"),
        State(abbreviation: "WA", name: "Washington"),
        State(abbreviation: "WV", name: "West Virginia"),
        State(abbreviation: "WI", name: "Wisconsin"),
        State(abbreviation: "WY", name: "Wyoming"),
        State(abbreviation: "DC", name: "Washington D.C."),
        State(abbreviation: "PR", name: "Puerto Rico"),
        State(abbreviation: "GU", name: "Guam"),
        State(abbreviation: "VI", name: "U.S. Virgin Islands"),
    ]
}
