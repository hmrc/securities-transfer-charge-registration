
# securities-transfer-charge-registration

# Functional design
This service provides the ability to authorise user groups to use our service.

Authenticated users who complete the onboarding process do so on behalf of their group. They must be administrative users.

In the case of individual users, the user and group are synonymous and the user is an admin.

In the case of organisations, the user onboards their group and their user within that group. Other users in the group can then be granted access by their admin without going through this process again.

The sign-up design differs between individuals, organisations and agencies due to the differing systems involved.

## Individuals
For individuals, we have to first check their confidence level. If it is below 250, we will have to take them though IV Uplift to get their level up. Once that's done, we will need to manually orchestrate calls to register, subscribe and enrol. It's not clear at this point if we will need a UI, but the expectation is that we will.

## Organisations
For organisations, we expect to be able to use GRS.

## Agencies
We expect agents to be signed up via ASA. We will have to supply ASA with our Registration/Subscription APIs and they will orchestrate the sign-up process.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").