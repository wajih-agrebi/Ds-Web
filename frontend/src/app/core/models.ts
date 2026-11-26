export type FuelType = 'ESSENCE' | 'DIESEL' | 'HYBRIDE' | 'ELECTRIQUE';
export type Gearbox = 'MANUELLE' | 'AUTOMATIQUE';
export type VehicleStatus = 'DISPONIBLE' | 'LOUE' | 'MAINTENANCE';
export type VehicleCategory = 'ECONOMIQUE' | 'COMPACTE' | 'BERLINE' | 'SUV' | 'UTILITAIRE' | 'AUTRE';
export type CustomerCategory = 'STANDARD' | 'FIDELE' | 'VIP';
export type RentalStatus = 'RESERVEE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE';
export interface Vehicle { id: number; immatriculation: string; marque: string; modele: string; categorie: VehicleCategory; annee: number; couleur: string; carburant: FuelType; boite: Gearbox; nbPlaces: number; kilometrage: number; prixJournalier: number; statut: VehicleStatus; imageUrl?: string | null; }
export type VehicleInput = Omit<Vehicle, 'id'>;
export interface Client { id: number; cin: string; nom: string; prenom: string; dateNaissance: string; email: string; telephone: string; adresse: string; numeroPermis: string; dateExpirationPermis: string; categorieClient: CustomerCategory; dateInscription: string; }
export type ClientInput = Omit<Client, 'id' | 'categorieClient' | 'dateInscription'>;
export interface Eligibility { clientId: number; eligible: boolean; age: number; permisValide: boolean; categorieClient: CustomerCategory; message: string; }
export interface Rental { id: number; clientId: number; vehiculeId: number; dateDebut: string; dateFinPrevue: string; dateRetourEffectif: string | null; kmDepart: number | null; kmRetour: number | null; prixJournalierApplique: number; montantTotal: number; penalite: number; statut: RentalStatus; dateCreation: string; }
export interface RentalPage { content: Rental[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface PriceQuote { nombreJours: number; prixJournalier: number; coefficientCategorie: number; remiseFidelite: number; montantEstime: number; categorieClient: CustomerCategory; }
export interface RentalStats { chiffreAffairesTotal: number; chiffreAffairesMois: number; tauxOccupation: number; vehiculesLesPlusLoues: { vehiculeId: number; nombreLocations: number }[]; }
export interface EnrichedRental { location: Rental; client: Client; vehicule: Vehicle; }
