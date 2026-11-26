import { Vehicle } from './models';

const vehiclePhotoCatalog: { match: RegExp; url: string }[] = [
  {
    match: /toyota.*yaris|yaris.*toyota/i,
    url: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/90/2020-2024_Toyota_Yaris.jpg/960px-2020-2024_Toyota_Yaris.jpg',
  },
  {
    match: /renault.*clio|clio.*renault/i,
    url: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fb/2024_Renault_Clio_1.6_E-Tech_Hybrid_espirit_Alpine_in_Iron_Blue_at_Renault_Manchester_03.jpg/960px-2024_Renault_Clio_1.6_E-Tech_Hybrid_espirit_Alpine_in_Iron_Blue_at_Renault_Manchester_03.jpg',
  },
  {
    match: /peugeot.*3008|3008.*peugeot/i,
    url: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/12/2024_Peugeot_3008.jpg/960px-2024_Peugeot_3008.jpg',
  },
];

export function suggestedVehiclePhoto(vehicle: Pick<Vehicle, 'marque' | 'modele'>): string | null {
  const name = `${vehicle.marque} ${vehicle.modele}`.trim();
  return vehiclePhotoCatalog.find(photo => photo.match.test(name))?.url ?? null;
}
