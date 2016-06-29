function [ spec_perms ] = combSelect( n, k, numCombs )
    mx = nchoosek(n, k);
    randSelect = randperm(mx, numCombs);
    spec_perms = zeros(numCombs, k, 'single');
    parfor i=1:numCombs
       spec_perms(i,:) = combFind(n,k,randSelect(i)); 
    end
end

