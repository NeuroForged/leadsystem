package com.neuroforged.leadsystem.config;

import com.neuroforged.leadsystem.entity.*;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "neuroforged.seed-sample-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SampleDataSeeder implements ApplicationRunner {

    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final CalendlyMeetingRepository meetingRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clientRepository.count() > 0) {
            log.info("Clients already present — skipping sample data seed.");
            return;
        }
        log.info("Seeding sample data...");
        List<Client> clients = seedClients();
        List<Lead> leads = seedLeads(clients);
        backdateLeadTimestamps(leads);
        seedMeetings(clients, leads);
        log.info("Sample data seeded: {} clients, {} leads, {} meetings",
                clients.size(), leads.size(), meetingRepository.count());
    }

    private List<Client> seedClients() {
        List<Client> clients = new ArrayList<>();

        Client c1 = new Client();
        c1.setName("Peak Performance Gym");
        c1.setPrimaryEmail("contact@peakperformancegym.com");
        c1.setNotificationEmails("contact@peakperformancegym.com,owner@peakperformancegym.com");
        c1.setWebsiteUrl("https://peakperformancegym.com");
        clients.add(clientRepository.save(c1));

        Client c2 = new Client();
        c2.setName("Digital Spark Agency");
        c2.setPrimaryEmail("hello@digitalspark.io");
        c2.setNotificationEmails("hello@digitalspark.io");
        c2.setWebsiteUrl("https://digitalspark.io");
        clients.add(clientRepository.save(c2));

        Client c3 = new Client();
        c3.setName("Coastal Real Estate Group");
        c3.setPrimaryEmail("leads@coastalrealestate.com");
        c3.setNotificationEmails("leads@coastalrealestate.com,sales@coastalrealestate.com");
        c3.setWebsiteUrl("https://coastalrealestate.com");
        clients.add(clientRepository.save(c3));

        Client c4 = new Client();
        c4.setName("TechNovate Solutions");
        c4.setPrimaryEmail("sales@technovate.co");
        c4.setNotificationEmails("sales@technovate.co");
        c4.setWebsiteUrl("https://technovate.co");
        clients.add(clientRepository.save(c4));

        Client c5 = new Client();
        c5.setName("Bloom & Co Florals");
        c5.setPrimaryEmail("hello@bloomandco.com");
        c5.setNotificationEmails("hello@bloomandco.com");
        c5.setWebsiteUrl("https://bloomandco.com");
        clients.add(clientRepository.save(c5));

        return clients;
    }

    private List<Lead> seedLeads(List<Client> clients) {
        List<Lead> all = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Peak Performance Gym — 22 leads
        Client gym = clients.get(0);
        String gymId = String.valueOf(gym.getId());
        all.addAll(List.of(
            lead("james.miller@fitlife.com", "James Miller", "FitLife Studios", "Health & Wellness", "B2C", "Social Media", 120, 3.2, 45.0, 2800.0, 78, "Struggling to convert social followers into paying members.", gymId, LeadStatus.QUALIFIED, now.minusDays(5)),
            lead("sarah.chen@crossfitcore.com", "Sarah Chen", "CrossFit Core", "Health & Wellness", "B2C", "Referral", 200, 4.1, 38.0, 3200.0, 91, "Members drop off after 3 months — need better retention strategy.", gymId, LeadStatus.BOOKED, now.minusDays(9)),
            lead("mark.donovan@ironhousegym.com", "Mark Donovan", "Iron House Gym", "Health & Wellness", "B2C", "Organic Search", 90, 2.8, 52.0, 2500.0, 65, "Website traffic doesn't convert — missing a clear CTA.", gymId, LeadStatus.CONTACTED, now.minusDays(14)),
            lead("lisa.nguyen@yogaflow.com", "Lisa Nguyen", "YogaFlow Studio", "Health & Wellness", "B2C", "Paid Ads", 60, 5.5, 30.0, 1800.0, 82, "Google Ads getting expensive, ROI is dropping.", gymId, LeadStatus.QUALIFIED, now.minusDays(18)),
            lead("tom.baker@boxingpro.com", "Tom Baker", "Boxing Pro Club", "Health & Wellness", "B2C", "Social Media", 150, 2.1, 60.0, 3500.0, 55, "Not enough awareness in local area.", gymId, LeadStatus.NEW, now.minusDays(22)),
            lead("emily.ross@pilatesbarre.com", "Emily Ross", "Pilates & Barre Co", "Health & Wellness", "B2C", "Email Campaign", 80, 6.0, 25.0, 2200.0, 88, "Email list is cold — open rates under 10%.", gymId, LeadStatus.BOOKED, now.minusDays(26)),
            lead("ryan.patel@spinzone.com", "Ryan Patel", "Spin Zone Cycling", "Health & Wellness", "B2C", "Organic Search", 100, 3.5, 42.0, 2900.0, 73, "Competing against Peloton and losing. Need differentiation.", gymId, LeadStatus.QUALIFIED, now.minusDays(30)),
            lead("amanda.wilson@bootcampsq.com", "Amanda Wilson", "Bootcamp Squad", "Health & Wellness", "B2C", "Referral", 180, 4.8, 35.0, 3100.0, 85, "Word of mouth isn't scaling. Want a systematic referral program.", gymId, LeadStatus.CLOSED, now.minusDays(35)),
            lead("chris.hall@gymplus.com", "Chris Hall", "Gym Plus", "Health & Wellness", "B2B2C", "Direct", 250, 2.5, 55.0, 4000.0, 60, "Have 3 locations but leads don't flow between them.", gymId, LeadStatus.CONTACTED, now.minusDays(40)),
            lead("natalie.ford@zenfit.com", "Natalie Ford", "ZenFit Wellness", "Health & Wellness", "B2C", "Paid Ads", 70, 4.2, 48.0, 2100.0, 79, "Facebook ads cost too much. Exploring alternatives.", gymId, LeadStatus.NEW, now.minusDays(45)),
            lead("derek.james@sportshub.com", "Derek James", "Sports Hub", "Health & Wellness", "B2C", "Social Media", 300, 1.8, 65.0, 5000.0, 48, "Big audience but zero conversion online.", gymId, LeadStatus.NEW, now.minusDays(50)),
            lead("jessica.green@motionfitness.com", "Jessica Green", "Motion Fitness", "Health & Wellness", "B2C", "Organic Search", 110, 3.9, 40.0, 2600.0, 83, "Leads come in but sales team can't follow up fast enough.", gymId, LeadStatus.BOOKED, now.minusDays(55)),
            lead("brian.murphy@performfit.com", "Brian Murphy", "PerformFit", "Health & Wellness", "B2B", "Email Campaign", 90, 5.0, 30.0, 3800.0, 90, "Corporate wellness contracts — need a professional lead system.", gymId, LeadStatus.CLOSED, now.minusDays(60)),
            lead("karen.lee@activelife.com", "Karen Lee", "Active Life Center", "Health & Wellness", "B2C", "Referral", 140, 3.3, 44.0, 2700.0, 71, "Seasonal drop-off each winter is killing revenue.", gymId, LeadStatus.CONTACTED, now.minusDays(65)),
            lead("david.scott@trainerx.com", "David Scott", "TrainerX", "Health & Wellness", "B2B", "Direct", 50, 8.0, 20.0, 6000.0, 87, "Personal training franchise — need scalable lead gen.", gymId, LeadStatus.BOOKED, now.minusDays(70)),
            lead("michelle.white@sweatclub.com", "Michelle White", "Sweat Club", "Health & Wellness", "B2C", "Paid Ads", 130, 2.9, 50.0, 2400.0, 62, "Too dependent on Instagram — platform changes hurt us badly.", gymId, LeadStatus.NEW, now.minusDays(74)),
            lead("jason.carter@primefit.com", "Jason Carter", "Prime Fitness", "Health & Wellness", "B2C", "Organic Search", 95, 4.4, 36.0, 2950.0, 77, "Conversion rate dropped 40% after website redesign.", gymId, LeadStatus.QUALIFIED, now.minusDays(78)),
            lead("stephanie.adams@bodyworks.com", "Stephanie Adams", "BodyWorks Studio", "Health & Wellness", "B2C", "Social Media", 160, 2.2, 58.0, 3300.0, 53, "High churn — members leave after first month.", gymId, LeadStatus.NEW, now.minusDays(82)),
            lead("paul.robinson@elitefit.com", "Paul Robinson", "Elite Fit", "Health & Wellness", "B2B", "Referral", 200, 5.5, 28.0, 4500.0, 93, "Want to add AI chatbot to website to capture after-hours leads.", gymId, LeadStatus.CLOSED, now.minusDays(86)),
            lead("anna.martinez@fitstyle.com", "Anna Martinez", "FitStyle Gym", "Health & Wellness", "B2C", "Email Campaign", 85, 3.7, 43.0, 2550.0, 68, "Email campaigns get ignored. Need a smarter nurture flow.", gymId, LeadStatus.CONTACTED, now.minusDays(89)),
            lead("luke.thompson@powerhouse.com", "Luke Thompson", "PowerHouse Fitness", "Health & Wellness", "B2C", "Paid Ads", 175, 2.7, 54.0, 3600.0, 58, "Paid ads drain budget. No idea which campaigns actually convert.", gymId, LeadStatus.NEW, now.minusDays(91)),
            lead("olivia.harris@mindandbody.com", "Olivia Harris", "Mind & Body Studio", "Health & Wellness", "B2C", "Organic Search", 70, 6.1, 22.0, 1900.0, 86, "Great product, terrible online visibility. Need SEO + chatbot.", gymId, LeadStatus.QUALIFIED, now.minusDays(87))
        ));

        // Digital Spark Agency — 22 leads
        Client agency = clients.get(1);
        String agencyId = String.valueOf(agency.getId());
        all.addAll(List.of(
            lead("alex.turner@brightbrand.com", "Alex Turner", "Bright Brand Co", "Marketing Agency", "B2B", "Paid Ads", 50, 8.5, 120.0, 8000.0, 89, "Spending $15k/month on ads with no tracking or attribution.", agencyId, LeadStatus.BOOKED, now.minusDays(3)),
            lead("nina.patel@growthhaus.com", "Nina Patel", "GrowthHaus", "SaaS", "B2B", "Organic Search", 30, 12.0, 150.0, 12000.0, 94, "PLG motion isn't converting trial users. Need a nurture bot.", agencyId, LeadStatus.CLOSED, now.minusDays(7)),
            lead("sam.kowalski@clickcraft.com", "Sam Kowalski", "ClickCraft Digital", "Marketing Agency", "B2B", "Referral", 80, 6.0, 95.0, 7500.0, 76, "Clients keep asking for chatbots. We want to white-label a solution.", agencyId, LeadStatus.QUALIFIED, now.minusDays(11)),
            lead("rachel.hayes@localbloom.com", "Rachel Hayes", "LocalBloom Marketing", "Marketing Agency", "B2B", "Social Media", 45, 7.2, 110.0, 6800.0, 81, "Can't keep up with inbound — qualify 100+ leads a month manually.", agencyId, LeadStatus.BOOKED, now.minusDays(15)),
            lead("carlos.mendez@pixelpush.com", "Carlos Mendez", "PixelPush Studio", "Creative Agency", "B2B", "Email Campaign", 35, 9.0, 135.0, 9500.0, 72, "Creative studio expanding into digital marketing. Need lead system.", agencyId, LeadStatus.QUALIFIED, now.minusDays(19)),
            lead("tara.brooks@scalewire.com", "Tara Brooks", "ScaleWire", "SaaS", "B2B", "Direct", 20, 15.0, 200.0, 15000.0, 97, "Series A funded, need enterprise lead qualification at scale.", agencyId, LeadStatus.CLOSED, now.minusDays(23)),
            lead("oliver.grant@fusionmedia.com", "Oliver Grant", "Fusion Media Group", "Marketing Agency", "B2B", "Organic Search", 60, 5.5, 130.0, 8200.0, 69, "Lost 3 big clients last quarter. Need to rebuild the pipeline.", agencyId, LeadStatus.CONTACTED, now.minusDays(27)),
            lead("jade.williams@connective.com", "Jade Williams", "Connective Digital", "Marketing Agency", "B2B", "Referral", 40, 10.0, 115.0, 10500.0, 88, "Want AI-powered intake forms that pre-qualify and book calls.", agencyId, LeadStatus.BOOKED, now.minusDays(31)),
            lead("ryan.foster@launchspace.com", "Ryan Foster", "LaunchSpace", "SaaS", "B2B", "Paid Ads", 25, 14.0, 180.0, 13000.0, 92, "Onboarding drop-off at 40%. Need smarter lead qualification upfront.", agencyId, LeadStatus.QUALIFIED, now.minusDays(36)),
            lead("priya.nair@visibleroi.com", "Priya Nair", "VisibleROI Agency", "Marketing Agency", "B2B", "Social Media", 55, 7.8, 100.0, 7200.0, 74, "Running campaigns for 20 clients but tracking manually in sheets.", agencyId, LeadStatus.CONTACTED, now.minusDays(41)),
            lead("ethan.cole@forgespark.com", "Ethan Cole", "ForgeSpark Marketing", "Marketing Agency", "B2B", "Email Campaign", 65, 6.5, 90.0, 6500.0, 66, "Email list of 5k but 2% open rate. Something is broken.", agencyId, LeadStatus.NEW, now.minusDays(46)),
            lead("mia.santos@bluepulse.com", "Mia Santos", "BluePulse Digital", "Marketing Agency", "B2B", "Direct", 30, 11.0, 160.0, 11000.0, 85, "Referral pipeline is strong but we lose them in discovery.", agencyId, LeadStatus.BOOKED, now.minusDays(51)),
            lead("noah.james@starterspark.com", "Noah James", "StarterSpark", "SaaS", "B2B", "Organic Search", 15, 18.0, 250.0, 18000.0, 96, "Early-stage SaaS. Every lead matters. Need near-perfect qualification.", agencyId, LeadStatus.CLOSED, now.minusDays(56)),
            lead("ella.cook@agencydrive.com", "Ella Cook", "AgencyDrive", "Marketing Agency", "B2B", "Paid Ads", 70, 5.0, 105.0, 7000.0, 60, "Running paid for 8 clients but half the leads are unqualified.", agencyId, LeadStatus.NEW, now.minusDays(60)),
            lead("liam.ward@momentumagency.com", "Liam Ward", "Momentum Agency", "Marketing Agency", "B2B", "Referral", 50, 9.5, 125.0, 9000.0, 83, "Growing fast, team can't handle manual qualification anymore.", agencyId, LeadStatus.QUALIFIED, now.minusDays(64)),
            lead("grace.kim@rankrise.com", "Grace Kim", "RankRise SEO", "Marketing Agency", "B2B", "Organic Search", 90, 4.5, 80.0, 5500.0, 57, "SEO agency getting leads from their own Google rankings but no follow-up system.", agencyId, LeadStatus.NEW, now.minusDays(68)),
            lead("zach.thomas@amplifyiq.com", "Zach Thomas", "AmplifyIQ", "SaaS", "B2B", "Social Media", 20, 16.0, 220.0, 16000.0, 95, "B2B SaaS. High ACV. Need to ensure only enterprise-fit leads hit sales.", agencyId, LeadStatus.QUALIFIED, now.minusDays(72)),
            lead("sofia.diaz@conversionlab.com", "Sofia Diaz", "Conversion Lab", "Marketing Agency", "B2B", "Email Campaign", 45, 8.0, 110.0, 8500.0, 79, "CRO agency — practice what they preach. Optimising own lead flow.", agencyId, LeadStatus.CONTACTED, now.minusDays(76)),
            lead("henry.moore@brandaxis.com", "Henry Moore", "BrandAxis", "Creative Agency", "B2B", "Direct", 35, 10.5, 145.0, 10000.0, 70, "Rebranding services. Long sales cycle — need persistent nurturing.", agencyId, LeadStatus.CONTACTED, now.minusDays(80)),
            lead("ava.clark@outreachhq.com", "Ava Clark", "OutreachHQ", "SaaS", "B2B", "Paid Ads", 60, 7.0, 130.0, 11500.0, 84, "Outreach SaaS. Want to qualify leads before demo to save sales time.", agencyId, LeadStatus.NEW, now.minusDays(84)),
            lead("charlie.hughes@pilotgrowth.com", "Charlie Hughes", "PilotGrowth", "Marketing Agency", "B2B", "Referral", 40, 11.5, 115.0, 8800.0, 77, "Boutique agency, high-touch. Need pre-qualification to protect time.", agencyId, LeadStatus.NEW, now.minusDays(88)),
            lead("isabelle.scott@launchpad.com", "Isabelle Scott", "LaunchPad Marketing", "Marketing Agency", "B2B", "Organic Search", 55, 6.8, 95.0, 7300.0, 63, "First CRM — currently doing everything in Gmail.", agencyId, LeadStatus.NEW, now.minusDays(90))
        ));

        // Coastal Real Estate — 15 leads
        Client realty = clients.get(2);
        String realtyId = String.valueOf(realty.getId());
        all.addAll(List.of(
            lead("michael.brown@suncoastrealty.com", "Michael Brown", "Suncoast Realty", "Real Estate", "B2C", "Paid Ads", 30, 5.0, 200.0, 15000.0, 86, "Buyers browsing but not booking viewings. Need chatbot on listings.", realtyId, LeadStatus.BOOKED, now.minusDays(4)),
            lead("jessica.kim@bayviewhomes.com", "Jessica Kim", "BayView Homes", "Real Estate", "B2C", "Organic Search", 25, 6.5, 180.0, 18000.0, 92, "Capturing buyer info via Zillow but no own pipeline. Want independence.", realtyId, LeadStatus.CLOSED, now.minusDays(8)),
            lead("daniel.evans@harbourestate.com", "Daniel Evans", "Harbour Estate Agency", "Real Estate", "B2B", "Referral", 40, 4.2, 220.0, 22000.0, 80, "Managing 8 agents. Each has their own system — total chaos.", realtyId, LeadStatus.QUALIFIED, now.minusDays(13)),
            lead("patricia.morgan@oceanfront.com", "Patricia Morgan", "Oceanfront Properties", "Real Estate", "B2C", "Social Media", 20, 8.0, 150.0, 20000.0, 75, "Instagram tours get views but zero enquiries. Missing a call to action.", realtyId, LeadStatus.CONTACTED, now.minusDays(20)),
            lead("kevin.turner@landmarkprop.com", "Kevin Turner", "Landmark Properties", "Real Estate", "B2B", "Direct", 50, 3.8, 250.0, 25000.0, 68, "Developers looking to sell off-plan. Need investor lead capture.", realtyId, LeadStatus.CONTACTED, now.minusDays(28)),
            lead("linda.harris@sunriserealty.com", "Linda Harris", "Sunrise Realty", "Real Estate", "B2C", "Paid Ads", 35, 5.5, 190.0, 17000.0, 83, "Google Ads running but no way to track which leads actually close.", realtyId, LeadStatus.BOOKED, now.minusDays(35)),
            lead("james.martin@crestviewhomes.com", "James Martin", "Crestview Homes", "Real Estate", "B2C", "Email Campaign", 28, 7.0, 170.0, 16000.0, 77, "Monthly newsletter to past clients — want to re-activate dormant leads.", realtyId, LeadStatus.QUALIFIED, now.minusDays(43)),
            lead("margaret.walker@primeproperties.com", "Margaret Walker", "Prime Properties", "Real Estate", "B2B", "Organic Search", 45, 4.0, 230.0, 24000.0, 71, "Commercial property — corporate buyers need bespoke qualification.", realtyId, LeadStatus.NEW, now.minusDays(50)),
            lead("robert.hall@coastalcottages.com", "Robert Hall", "Coastal Cottages", "Real Estate", "B2C", "Referral", 22, 9.0, 160.0, 19000.0, 89, "Holiday let / short-term rental. Buyers want specific criteria.", realtyId, LeadStatus.CLOSED, now.minusDays(57)),
            lead("helen.wright@riverpointrealty.com", "Helen Wright", "Riverpoint Realty", "Real Estate", "B2C", "Social Media", 30, 6.0, 185.0, 17500.0, 64, "TikTok property tours going viral but can't capture the leads.", realtyId, LeadStatus.NEW, now.minusDays(63)),
            lead("william.jones@eliteestate.com", "William Jones", "Elite Estate Agents", "Real Estate", "B2B", "Paid Ads", 55, 3.5, 260.0, 28000.0, 56, "Premium listings. Serious buyers only — currently drowning in tyre-kickers.", realtyId, LeadStatus.NEW, now.minusDays(70)),
            lead("barbara.clark@greenvallley.com", "Barbara Clark", "Green Valley Estates", "Real Estate", "B2C", "Direct", 18, 10.0, 140.0, 21000.0, 91, "Eco-property niche. Highly motivated buyers but hard to find.", realtyId, LeadStatus.BOOKED, now.minusDays(76)),
            lead("thomas.lewis@newhoriz.com", "Thomas Lewis", "New Horizons Realty", "Real Estate", "B2C", "Organic Search", 38, 4.8, 200.0, 16500.0, 73, "Ranking well on Google but bounce rate at 78%. Need chatbot.", realtyId, LeadStatus.CONTACTED, now.minusDays(82)),
            lead("diana.robinson@prestige.com", "Diana Robinson", "Prestige Homes", "Real Estate", "B2B", "Referral", 42, 5.2, 210.0, 23000.0, 79, "High-net-worth clients referred by financial advisors. Need white-glove intake.", realtyId, LeadStatus.QUALIFIED, now.minusDays(87)),
            lead("george.scott@pointview.com", "George Scott", "Pointview Property", "Real Estate", "B2C", "Email Campaign", 25, 7.5, 175.0, 15500.0, 66, "Email list from past open-homes. Sitting idle for 2 years.", realtyId, LeadStatus.NEW, now.minusDays(90))
        ));

        // TechNovate Solutions — 18 leads
        Client tech = clients.get(3);
        String techId = String.valueOf(tech.getId());
        all.addAll(List.of(
            lead("victoria.young@nexustech.com", "Victoria Young", "Nexus Tech", "SaaS", "B2B", "Paid Ads", 15, 20.0, 300.0, 25000.0, 95, "Enterprise SaaS with $50k ACV. Every unqualified demo is $5k wasted.", techId, LeadStatus.BOOKED, now.minusDays(2)),
            lead("aaron.price@cloudshift.com", "Aaron Price", "CloudShift", "SaaS", "B2B", "Organic Search", 10, 25.0, 400.0, 35000.0, 98, "Series B SaaS. Need to 3x pipeline without 3x-ing SDR headcount.", techId, LeadStatus.CLOSED, now.minusDays(6)),
            lead("natasha.jenkins@datafuse.com", "Natasha Jenkins", "DataFuse", "SaaS", "B2B", "Referral", 20, 18.0, 280.0, 22000.0, 88, "Data platform. ICP is mid-market finance. Chatbot to pre-qualify vertical.", techId, LeadStatus.QUALIFIED, now.minusDays(10)),
            lead("felix.owens@stackflow.com", "Felix Owens", "StackFlow", "SaaS", "B2B", "Social Media", 18, 15.0, 320.0, 28000.0, 82, "Developer tools company. LinkedIn outbound isn't converting. Need inbound.", techId, LeadStatus.BOOKED, now.minusDays(16)),
            lead("marina.black@gridlogic.com", "Marina Black", "GridLogic", "SaaS", "B2B", "Email Campaign", 12, 22.0, 350.0, 30000.0, 90, "Logistics SaaS. Long 6-month sales cycle. Need to qualify intent early.", techId, LeadStatus.CLOSED, now.minusDays(21)),
            lead("jake.hall@softnest.com", "Jake Hall", "SoftNest", "SaaS", "B2B", "Direct", 25, 12.0, 240.0, 18000.0, 74, "HR tech startup. Demo requests mostly from wrong ICP.", techId, LeadStatus.QUALIFIED, now.minusDays(25)),
            lead("claire.adams@voltiq.com", "Claire Adams", "VoltIQ", "SaaS", "B2B", "Organic Search", 8, 28.0, 500.0, 45000.0, 97, "AI energy analytics. Enterprise buyers only. Gatekeeping is critical.", techId, LeadStatus.BOOKED, now.minusDays(29)),
            lead("brendan.kelly@loopframe.com", "Brendan Kelly", "LoopFrame", "SaaS", "B2B", "Paid Ads", 22, 14.0, 270.0, 20000.0, 78, "No-code automation. Want to capture SMB and enterprise in same funnel differently.", techId, LeadStatus.QUALIFIED, now.minusDays(33)),
            lead("yuki.tanaka@signalcore.com", "Yuki Tanaka", "SignalCore", "SaaS", "B2B", "Referral", 14, 19.0, 310.0, 26000.0, 86, "Sales intelligence SaaS. Ironic that their own prospecting is manual.", techId, LeadStatus.CONTACTED, now.minusDays(38)),
            lead("petra.vasquez@orbitmatch.com", "Petra Vasquez", "OrbitMatch", "SaaS", "B2B", "Social Media", 30, 10.0, 200.0, 14000.0, 65, "Marketplace SaaS with two-sided demand. Complex qualification needed.", techId, LeadStatus.NEW, now.minusDays(44)),
            lead("henry.chang@boltpay.com", "Henry Chang", "BoltPay", "Fintech", "B2B", "Email Campaign", 16, 17.0, 290.0, 23000.0, 81, "Payments SaaS. Compliance-heavy onboarding needs smart pre-screening.", techId, LeadStatus.BOOKED, now.minusDays(48)),
            lead("leila.okafor@depthanalytics.com", "Leila Okafor", "Depth Analytics", "SaaS", "B2B", "Direct", 11, 23.0, 380.0, 32000.0, 93, "BI tool targeting CFOs. Need chatbot that speaks finance language.", techId, LeadStatus.CLOSED, now.minusDays(53)),
            lead("miles.burton@flowsync.com", "Miles Burton", "FlowSync", "SaaS", "B2B", "Organic Search", 28, 11.0, 220.0, 16000.0, 67, "Project management tool. Very saturated market. Differentiation is key.", techId, LeadStatus.CONTACTED, now.minusDays(58)),
            lead("anna.reed@cipherops.com", "Anna Reed", "CipherOps", "Cybersecurity", "B2B", "Referral", 9, 26.0, 450.0, 40000.0, 96, "Cybersecurity platform. Inbound from breach news cycles — need to capture fast.", techId, LeadStatus.BOOKED, now.minusDays(62)),
            lead("joel.sanchez@driftboard.com", "Joel Sanchez", "DriftBoard", "SaaS", "B2B", "Paid Ads", 35, 9.0, 180.0, 12000.0, 59, "Startup with big inbound. Not enough time to qualify everyone.", techId, LeadStatus.NEW, now.minusDays(66)),
            lead("fiona.cross@pulsecrm.com", "Fiona Cross", "PulseCRM", "SaaS", "B2B", "Social Media", 17, 16.0, 300.0, 24000.0, 84, "CRM vendor. Wants to use AI to displace traditional contact forms.", techId, LeadStatus.QUALIFIED, now.minusDays(71)),
            lead("omar.hassan@launchkit.com", "Omar Hassan", "LaunchKit", "SaaS", "B2B", "Email Campaign", 20, 13.0, 260.0, 19000.0, 72, "Product analytics. Free trial users not converting. Needs better qualification.", techId, LeadStatus.NEW, now.minusDays(75)),
            lead("elena.russo@sprintware.com", "Elena Russo", "SprintWare", "SaaS", "B2B", "Direct", 24, 12.5, 230.0, 17000.0, 69, "Dev tooling. Engineers submit interest but never have budget authority.", techId, LeadStatus.NEW, now.minusDays(85))
        ));

        // Bloom & Co Florals — 12 leads
        Client florals = clients.get(4);
        String floralsId = String.valueOf(florals.getId());
        all.addAll(List.of(
            lead("sophie.taylor@eventbloom.com", "Sophie Taylor", "EventBloom", "E-commerce", "B2B", "Social Media", 200, 3.5, 25.0, 3500.0, 76, "Event florist. 80% of bookings from Instagram DMs — not scalable.", floralsId, LeadStatus.QUALIFIED, now.minusDays(6)),
            lead("jack.davies@bloominbeauty.com", "Jack Davies", "Bloomin' Beauty", "Retail", "B2C", "Organic Search", 350, 2.8, 18.0, 800.0, 62, "Online flower shop. Cart abandonment at 70%. Need chatbot to re-engage.", floralsId, LeadStatus.CONTACTED, now.minusDays(12)),
            lead("emma.watson@weddingpetals.com", "Emma Watson", "Wedding Petals", "E-commerce", "B2B", "Referral", 150, 5.0, 30.0, 5000.0, 88, "Wedding florist. Enquiries spike in January. Need automated triage.", floralsId, LeadStatus.BOOKED, now.minusDays(17)),
            lead("harry.johnson@corporateblooms.com", "Harry Johnson", "Corporate Blooms", "E-commerce", "B2B", "Email Campaign", 120, 4.2, 35.0, 4200.0, 83, "B2B subscription florals. Corporates want quick quotes — slow response is losing deals.", floralsId, LeadStatus.BOOKED, now.minusDays(23)),
            lead("lucy.robinson@petalcraft.com", "Lucy Robinson", "PetalCraft Studio", "Retail", "B2C", "Paid Ads", 280, 2.2, 22.0, 950.0, 55, "Paid ads driving traffic but zero conversions. Site loads in 6 seconds.", floralsId, LeadStatus.NEW, now.minusDays(30)),
            lead("alfie.brown@seedtovase.com", "Alfie Brown", "Seed to Vase", "E-commerce", "B2C", "Direct", 420, 1.8, 15.0, 650.0, 47, "Subscription box service. 30-day free trial isn't converting to paid.", floralsId, LeadStatus.NEW, now.minusDays(38)),
            lead("poppy.wilson@luxepetal.com", "Poppy Wilson", "Luxe Petal", "Retail", "B2C", "Social Media", 180, 4.5, 28.0, 2800.0, 79, "High-end florist. Want to pre-qualify budget before booking consultation.", floralsId, LeadStatus.CLOSED, now.minusDays(44)),
            lead("oscar.moore@freshcutflorals.com", "Oscar Moore", "Fresh Cut Florals", "E-commerce", "B2B", "Organic Search", 260, 3.0, 20.0, 1500.0, 70, "Wholesale + retail. Need separate flows for trade vs consumer.", floralsId, LeadStatus.QUALIFIED, now.minusDays(52)),
            lead("rosie.martin@bloombox.com", "Rosie Martin", "BloomBox Subscriptions", "E-commerce", "B2C", "Email Campaign", 310, 2.5, 16.0, 720.0, 64, "Monthly subscription. High churn. Chatbot to catch cancellations.", floralsId, LeadStatus.NEW, now.minusDays(59)),
            lead("freddie.evans@springpetal.com", "Freddie Evans", "SpringPetal", "Retail", "B2C", "Paid Ads", 190, 3.8, 26.0, 1100.0, 73, "Seasonal peaks around Valentine's and Mother's Day drain capacity.", floralsId, LeadStatus.CONTACTED, now.minusDays(67)),
            lead("daisy.clark@artisanflorals.com", "Daisy Clark", "Artisan Florals", "E-commerce", "B2B", "Referral", 140, 5.5, 32.0, 4800.0, 91, "Luxury wedding market. Word of mouth is great but capacity maxed out.", floralsId, LeadStatus.CLOSED, now.minusDays(73)),
            lead("tommy.harris@wildflowerco.com", "Tommy Harris", "Wildflower Co", "Retail", "B2C", "Social Media", 400, 1.5, 12.0, 550.0, 42, "Large social following, zero website sales. Need to close the gap.", floralsId, LeadStatus.NEW, now.minusDays(88))
        ));

        return leadRepository.saveAll(all);
    }

    private Lead lead(String email, String firstName, String businessName, String businessType,
                      String customerType, String trafficSource, int monthlyLeads,
                      double conversionRate, double costPerLead, double clientValue,
                      int leadScore, String challenge, String clientId, LeadStatus status,
                      LocalDateTime createdAt) {
        return Lead.builder()
                .email(email)
                .firstName(firstName.split(" ")[0])
                .businessName(businessName)
                .businessType(businessType)
                .customerType(customerType)
                .trafficSource(trafficSource)
                .monthlyLeads(monthlyLeads)
                .conversionRate(conversionRate)
                .costPerLead(costPerLead)
                .clientValue(clientValue)
                .leadScore(leadScore)
                .leadChallenge(challenge)
                .clientId(clientId)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    private void backdateLeadTimestamps(List<Lead> leads) {
        for (Lead lead : leads) {
            if (lead.getCreatedAt() != null) {
                jdbcTemplate.update(
                        "UPDATE lead SET created_at = ? WHERE id = ?",
                        lead.getCreatedAt(), lead.getId()
                );
            }
        }
    }

    private void seedMeetings(List<Client> clients, List<Lead> leads) {
        LocalDateTime now = LocalDateTime.now();
        ZoneId utc = ZoneId.of("UTC");
        List<CalendlyMeeting> meetings = new ArrayList<>();

        // Helper: find leads for a client that are BOOKED or CLOSED
        java.util.function.BiFunction<Client, LeadStatus, List<Lead>> clientLeads = (client, status) ->
                leads.stream()
                     .filter(l -> l.getClientId().equals(String.valueOf(client.getId())) && l.getStatus() == status)
                     .toList();

        // Peak Performance Gym
        Client gym = clients.get(0);
        for (Lead l : clientLeads.apply(gym, LeadStatus.BOOKED)) {
            meetings.add(meeting(gym, l, "Discovery Call - " + l.getBusinessName(), now.plusDays(meetings.size() % 5 + 1), 30, MeetingStatus.SCHEDULED, utc));
        }
        for (Lead l : clientLeads.apply(gym, LeadStatus.CLOSED)) {
            meetings.add(meeting(gym, l, "Strategy Session - " + l.getBusinessName(), now.minusDays(10 + meetings.size() % 20), 45, MeetingStatus.SCHEDULED, utc));
        }

        // Digital Spark Agency
        Client agency = clients.get(1);
        for (Lead l : clientLeads.apply(agency, LeadStatus.BOOKED)) {
            int offset = meetings.size() % 8;
            meetings.add(meeting(agency, l, "Intro Call - " + l.getBusinessName(), now.plusDays(offset + 1), 30, MeetingStatus.SCHEDULED, utc));
        }
        for (Lead l : clientLeads.apply(agency, LeadStatus.CLOSED)) {
            meetings.add(meeting(agency, l, "Demo - " + l.getBusinessName(), now.minusDays(5 + meetings.size() % 25), 60, MeetingStatus.SCHEDULED, utc));
        }
        // A couple of no-shows and cancellations for realism
        List<Lead> agencyQualified = leads.stream()
                .filter(l -> l.getClientId().equals(String.valueOf(agency.getId())) && l.getStatus() == LeadStatus.QUALIFIED)
                .limit(2).toList();
        if (!agencyQualified.isEmpty()) {
            meetings.add(meeting(agency, agencyQualified.get(0), "Cancelled Call - " + agencyQualified.get(0).getBusinessName(), now.minusDays(8), 30, MeetingStatus.CANCELLED, utc));
        }
        if (agencyQualified.size() > 1) {
            meetings.add(meeting(agency, agencyQualified.get(1), "No Show - " + agencyQualified.get(1).getBusinessName(), now.minusDays(15), 30, MeetingStatus.NO_SHOW, utc));
        }

        // Coastal Real Estate
        Client realty = clients.get(2);
        for (Lead l : clientLeads.apply(realty, LeadStatus.BOOKED)) {
            meetings.add(meeting(realty, l, "Property Viewing - " + l.getBusinessName(), now.plusDays(meetings.size() % 6 + 2), 60, MeetingStatus.SCHEDULED, utc));
        }
        for (Lead l : clientLeads.apply(realty, LeadStatus.CLOSED)) {
            meetings.add(meeting(realty, l, "Signed - " + l.getBusinessName(), now.minusDays(12 + meetings.size() % 15), 90, MeetingStatus.SCHEDULED, utc));
        }

        // TechNovate Solutions
        Client tech = clients.get(3);
        for (Lead l : clientLeads.apply(tech, LeadStatus.BOOKED)) {
            meetings.add(meeting(tech, l, "Technical Demo - " + l.getBusinessName(), now.plusDays(meetings.size() % 7 + 1), 60, MeetingStatus.SCHEDULED, utc));
        }
        for (Lead l : clientLeads.apply(tech, LeadStatus.CLOSED)) {
            meetings.add(meeting(tech, l, "Onboarding - " + l.getBusinessName(), now.minusDays(7 + meetings.size() % 30), 45, MeetingStatus.SCHEDULED, utc));
        }

        // Bloom & Co
        Client florals = clients.get(4);
        for (Lead l : clientLeads.apply(florals, LeadStatus.BOOKED)) {
            meetings.add(meeting(florals, l, "Consultation - " + l.getBusinessName(), now.plusDays(meetings.size() % 4 + 1), 45, MeetingStatus.SCHEDULED, utc));
        }
        for (Lead l : clientLeads.apply(florals, LeadStatus.CLOSED)) {
            meetings.add(meeting(florals, l, "Follow-up - " + l.getBusinessName(), now.minusDays(6 + meetings.size() % 20), 30, MeetingStatus.SCHEDULED, utc));
        }

        meetingRepository.saveAll(meetings);
    }

    private CalendlyMeeting meeting(Client client, Lead lead, String eventType,
                                    LocalDateTime startLocal, int durationMins,
                                    MeetingStatus status, ZoneId zone) {
        ZonedDateTime start = startLocal.atZone(zone);
        return CalendlyMeeting.builder()
                .client(client)
                .inviteeEmail(lead.getEmail())
                .inviteeName(lead.getFirstName())
                .eventType(eventType)
                .startTime(start)
                .endTime(start.plusMinutes(durationMins))
                .status(status)
                .calendlyUri("https://api.calendly.com/scheduled_events/seed-" + lead.getId() + "-" + client.getId())
                .build();
    }
}
